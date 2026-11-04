(() => {
    const form = document.getElementById('eventProposalForm');
    if (!form) return;
    const base = `http://${window.location.hostname}:8080/api/user/events`;
    const message = document.getElementById('proposalMessage');
    const listMessage = document.getElementById('myProposalsMessage');
    const list = document.getElementById('myProposalsContainer');
    const category = form.elements.idCategory;
    const submit = form.querySelector('[type="submit"]');

    async function request(path, options = {}) {
        const response = await fetch(base + path, {
            ...options, credentials: 'include',
            headers: { Accept: 'application/json', ...(options.headers || {}) }
        });
        if (response.status === 401 || response.status === 403) {
            throw new Error('Accès réservé aux étudiants connectés. Reconnecte-toi avec ton compte USER.');
        }
        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            throw new Error(error.message || 'Impossible de traiter la demande. Réessaie plus tard.');
        }
        return response.json();
    }
    function errorText(error) {
        return error instanceof TypeError ? 'Le serveur est inaccessible. Vérifie ta connexion et le démarrage de Spring Boot.' : error.message;
    }
    function node(tag, className, text) {
        const el = document.createElement(tag);
        el.className = className;
        el.textContent = text;
        return el;
    }
    async function loadMine() {
        try {
            const events = await request('/mine');
            if (!Array.isArray(events)) throw new Error('Réponse du serveur invalide.');
            list.replaceChildren(...events.map(event => {
                const card = node('article', 'event-card', '');
                const content = node('div', 'event-card-content', '');
                const labels = { EN_ATTENTE: 'En attente de validation', PUBLIE: 'Publié', REFUSE: 'Refusé' };
                content.append(node('h3', '', event.title),
                    node('p', 'event-date', event.eventDate),
                    node('p', 'event-location', event.location),
                    node('p', 'event-description', event.categoryName),
                    node('span', 'event-status', labels[event.status] || 'Statut inconnu'));
                card.append(content);
                return card;
            }));
            listMessage.textContent = events.length ? '' : 'Tu n’as pas encore proposé d’événement.';
        } catch (error) { listMessage.textContent = errorText(error); }
    }
    async function loadCategories() {
        try {
            const categories = await request('/categories');
            if (!Array.isArray(categories)) throw new Error('Réponse du serveur invalide.');
            category.replaceChildren(new Option('Choisir une catégorie', ''));
            categories.forEach(item => category.add(new Option(item.name, item.idCategory)));
            category.disabled = categories.length === 0;
            submit.disabled = categories.length === 0;
            if (!categories.length) message.textContent = 'Aucune catégorie disponible. Contacte l’administration.';
        } catch (error) { message.textContent = errorText(error); }
    }
    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (submit.disabled || !form.reportValidity()) return;
        const data = Object.fromEntries(new FormData(form));
        data.capacity = Number(data.capacity);
        data.idCategory = Number(data.idCategory);
        submit.disabled = true;
        message.textContent = 'Envoi en cours...';
        try {
            const result = await request('', {
                method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data)
            });
            message.textContent = result.message;
            form.reset();
            await loadMine();
        } catch (error) { message.textContent = errorText(error); }
        finally { submit.disabled = category.disabled; }
    });
    loadCategories();
    loadMine();
})();
