(() => {
    const usersBody = document.getElementById('adminUsers');
    if (!usersBody) return;
    const eventsBody = document.getElementById('adminEvents');
    const categories = document.getElementById('adminCategories');
    const search = document.getElementById('adminUserSearch');
    const filter = document.getElementById('adminStatusFilter');
    const message = document.getElementById('adminMessage');
    const refresh = document.getElementById('reloadAdmin');
    const stats = document.getElementById('adminStats');
    const counters = document.querySelectorAll('[data-stat]');
    let data = null;
    let busy = false;
    let action = null;
    let saving = false;
    const dialog = document.getElementById('userActionDialog');
    const actionForm = document.getElementById('userActionForm');
    const roleSelect = document.getElementById('userRoleSelect');
    const actionMessage = document.getElementById('userActionMessage');
    const confirmButton = document.getElementById('confirmUserAction');
    const cancelButton = document.getElementById('cancelUserAction');
    const addCategory = document.getElementById('addCategory');
    const categoryDialog = document.getElementById('categoryDialog');
    const categoryForm = document.getElementById('categoryForm');
    const categoryName = document.getElementById('categoryName');
    const categoryDescription = document.getElementById('categoryDescription');
    const categoryMessage = document.getElementById('categoryDialogMessage');
    const categoryConfirm = document.getElementById('confirmCategory');
    const categoryCancel = document.getElementById('cancelCategory');
    let categoryAction = null;
    async function categoryRequest(path, options = {}) {
        const response = await fetch(`${window.location.protocol === 'https:' ? window.location.origin : 'http://' + window.location.hostname + ':8080'}/api/admin/categories${path}`, {
            ...options, credentials: 'include', headers: { Accept: 'application/json', ...(options.headers || {}) }
        });
        const result = await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(result.message || (response.status === 401 || response.status === 403
            ? 'Accès réservé aux administrateurs connectés.' : 'Opération impossible. Réessayez plus tard.'));
        return result;
    }
    async function openCategory(kind, category = null) {
        if (busy || saving) return;
        if (kind === 'delete') {
            saving = true; renderCategories();
            try {
                const check = await categoryRequest(`/${category.idCategory}/deletion-check`);
                if (!check.deletable) { message.textContent = check.message; return; }
            } catch (error) { message.textContent = error instanceof TypeError ? 'Serveur inaccessible. Suppression non autorisée.' : error.message; return; }
            finally { saving = false; renderCategories(); }
        }
        categoryAction = { kind, category };
        document.getElementById('categoryDialogTitle').textContent = kind === 'create' ? 'Ajouter une catégorie' : kind === 'edit' ? 'Modifier la catégorie' : 'Supprimer la catégorie';
        document.getElementById('categoryDialogDetails').textContent = kind === 'delete'
            ? `Confirmez-vous la suppression de « ${category.name} » ? Le serveur vérifiera à nouveau qu’aucun événement ne l’utilise.`
            : 'Vérifiez les informations puis confirmez leur enregistrement.';
        document.getElementById('categoryFields').hidden = kind === 'delete';
        categoryName.disabled = kind === 'delete'; categoryDescription.disabled = kind === 'delete';
        categoryName.value = category?.name || ''; categoryDescription.value = category?.description || '';
        categoryMessage.textContent = '';
        categoryConfirm.textContent = kind === 'delete' ? 'Confirmer la suppression' : "Confirmer l'enregistrement";
        categoryDialog.showModal();
    }
    function renderCategories() {
        addCategory.disabled = busy || saving || !data;
        if (!data) return;
        categories.replaceChildren(...data.categories.map(category => {
            const card = node('article', null, 'moderator-panel');
            card.append(node('h3', category.name), node('p', category.description || 'Aucune description renseignée.'));
            const actions = node('div', null, 'admin-category-actions');
            for (const [kind, label] of [['edit', 'Modifier'], ['delete', 'Supprimer']]) {
                const button = node('button', label, 'secondary-button'); button.type = 'button'; button.disabled = busy || saving;
                button.addEventListener('click', () => openCategory(kind, category)); actions.append(button);
            }
            card.append(actions); return card;
        }));
        if (!data.categories.length) categories.append(node('p', 'Aucune catégorie enregistrée.'));
    }
    function openAction(user, kind) {
        if (busy || saving) return;
        action = { user, kind };
        document.getElementById('userActionTitle').textContent = kind === 'role' ? 'Modifier le rôle' : user.status === 'ACTIVE' ? 'Désactiver le compte' : 'Activer le compte';
        document.getElementById('userActionDetails').textContent = `${user.firstName} ${user.lastName} (${user.email}) · Rôle actuel : ${user.role}. Confirmez-vous cette modification des accès ?`;
        document.getElementById('userRoleLabel').hidden = kind !== 'role';
        roleSelect.value = user.role;
        actionMessage.textContent = '';
        dialog.showModal();
    }
    function node(tag, text, className = '') {
        const item = document.createElement(tag);
        item.className = className;
        if (text != null) item.textContent = text;
        return item;
    }
    function empty(body, columns, text) {
        const cell = node('td', text); cell.colSpan = columns;
        const row = node('tr'); row.append(cell); body.replaceChildren(row);
    }
    function rows(body, items, fields, label) {
        if (!items.length) { empty(body, fields.length, label); return; }
        body.replaceChildren(...items.map(item => {
            const row = node('tr');
            fields.forEach(field => row.append(node('td', field(item) ?? '—')));
            return row;
        }));
    }
    function renderUsers() {
        if (!data) return;
        const term = search.value.trim().toLocaleLowerCase('fr');
        const visible = data.users.filter(user => `${user.firstName} ${user.lastName} ${user.email} ${user.role} ${user.status}`.toLocaleLowerCase('fr').includes(term));
        if (!visible.length) { empty(usersBody, 5, 'Aucun utilisateur à afficher.'); return; }
        usersBody.replaceChildren(...visible.map(user => {
            const row = node('tr');
            [`${user.firstName} ${user.lastName}`, user.email, user.role, user.status].forEach(value => row.append(node('td', value)));
            const actions = node('td', null, 'admin-user-actions');
            for (const [kind, label] of [['role', 'Modifier le rôle'], ['status', user.status === 'ACTIVE' ? 'Désactiver' : 'Activer']]) {
                const button = node('button', label, 'secondary-button');
                button.type = 'button'; button.disabled = busy || saving;
                button.addEventListener('click', () => openAction(user, kind)); actions.append(button);
            }
            row.append(actions); return row;
        }));
    }
    function renderEvents() {
        if (!data) return;
        rows(eventsBody, data.events.filter(event => !filter.value || event.status === filter.value),
            [e => e.title, e => e.creatorName, e => e.categoryName, e => e.eventDate, e => e.status], 'Aucun événement pour ce statut.');
    }
    async function load() {
        if (busy) return;
        busy = true; refresh.disabled = true; stats.setAttribute('aria-busy', 'true');
        message.textContent = 'Chargement du tableau de bord...';
        try {
            const response = await fetch(`${window.location.protocol === 'https:' ? window.location.origin : 'http://' + window.location.hostname + ':8080'}/api/admin/dashboard`, {
                credentials: 'include', headers: { Accept: 'application/json' }
            });
            if (response.status === 401 || response.status === 403) throw new Error('Accès réservé aux administrateurs connectés. Vérifiez votre session.');
            if (!response.ok) throw new Error('Le tableau de bord est momentanément indisponible. Réessayez avec « Actualiser ».');
            const result = await response.json();
            if (!result.stats || !['users', 'categories', 'events'].every(key => Array.isArray(result[key]))
                || !Array.from(counters).every(counter => Number.isSafeInteger(result.stats[counter.dataset.stat]) && result.stats[counter.dataset.stat] >= 0)) {
                throw new Error('Réponse du serveur invalide. Réessayez dans quelques instants.');
            }
            data = result;
            counters.forEach(counter => { counter.textContent = String(data.stats[counter.dataset.stat]); });
            const selected = filter.value;
            filter.replaceChildren(new Option('Tous les statuts', ''));
            [...new Set(data.events.map(event => event.status))].sort().forEach(status => filter.add(new Option(status, status)));
            filter.value = [...filter.options].some(option => option.value === selected) ? selected : '';
            renderUsers(); renderEvents();
            renderCategories();
            message.textContent = 'Données à jour.';
        } catch (error) {
            data = null;
            counters.forEach(counter => { counter.textContent = '—'; });
            empty(usersBody, 5, 'Données indisponibles.'); empty(eventsBody, 5, 'Données indisponibles.');
            categories.replaceChildren(node('p', 'Données indisponibles.'));
            message.textContent = error instanceof TypeError ? 'Impossible de contacter le serveur. Vérifiez votre connexion et le démarrage de Spring Boot.' : error.message;
        } finally { busy = false; refresh.disabled = false; stats.setAttribute('aria-busy', 'false'); renderUsers(); renderCategories(); }
    }
    cancelButton.addEventListener('click', () => { if (!saving) dialog.close(); });
    dialog.addEventListener('cancel', event => { if (saving) event.preventDefault(); });
    actionForm.addEventListener('submit', async event => {
        event.preventDefault();
        if (saving || !action) return;
        saving = true; confirmButton.disabled = true; cancelButton.disabled = true; roleSelect.disabled = true;
        actionMessage.textContent = 'Modification en cours...';
        try {
            const { user, kind } = action;
            const value = kind === 'role' ? roleSelect.value : user.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
            const response = await fetch(`${window.location.protocol === 'https:' ? window.location.origin : 'http://' + window.location.hostname + ':8080'}/api/admin/users/${user.idUser}/${kind}`, {
                method: 'PATCH', credentials: 'include', headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
                body: JSON.stringify({ [kind]: value })
            });
            const result = await response.json().catch(() => ({}));
            if (!response.ok) throw new Error(result.message || 'Modification refusée. Vérifiez votre session et réessayez.');
            dialog.close();
            await load();
            message.textContent = data ? result.message : 'Modification enregistrée, mais actualisation impossible. Réessayez avec « Actualiser ».';
        } catch (error) { actionMessage.textContent = error instanceof TypeError ? 'Serveur inaccessible. Vérifiez le résultat avec « Actualiser » avant de réessayer.' : error.message; }
        finally { saving = false; confirmButton.disabled = false; cancelButton.disabled = false; roleSelect.disabled = false; renderUsers(); }
    });
    search.addEventListener('input', renderUsers);
    addCategory.addEventListener('click', () => openCategory('create'));
    categoryCancel.addEventListener('click', () => { if (!saving) categoryDialog.close(); });
    categoryDialog.addEventListener('cancel', event => { if (saving) event.preventDefault(); });
    categoryForm.addEventListener('submit', async event => {
        event.preventDefault();
        if (saving || !categoryAction || !categoryForm.reportValidity()) return;
        saving = true; categoryConfirm.disabled = true; categoryCancel.disabled = true;
        categoryMessage.textContent = 'Enregistrement en cours...'; renderCategories();
        try {
            const { kind, category } = categoryAction;
            const options = { method: kind === 'create' ? 'POST' : kind === 'edit' ? 'PATCH' : 'DELETE' };
            if (kind !== 'delete') {
                options.headers = { 'Content-Type': 'application/json' };
                options.body = JSON.stringify({ name: categoryName.value, description: categoryDescription.value });
            }
            const result = await categoryRequest(kind === 'create' ? '' : `/${category.idCategory}`, options);
            categoryDialog.close();
            await load();
            message.textContent = data ? result.message : 'Modification enregistrée, mais actualisation impossible. Utilisez « Actualiser ».';
        } catch (error) { categoryMessage.textContent = error instanceof TypeError ? 'Connexion interrompue. Vérifiez la liste avant de réessayer.' : error.message; }
        finally { saving = false; categoryConfirm.disabled = false; categoryCancel.disabled = false; renderCategories(); renderUsers(); }
    });
    filter.addEventListener('change', renderEvents);
    refresh.addEventListener('click', load);
    load();
})();
