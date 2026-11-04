(() => {
    const list = document.getElementById('pendingEvents');
    if (!list) return;
    const message = document.getElementById('moderationMessage');
    const refresh = document.getElementById('refreshPending');
    const base = `http://${window.location.hostname}:8080/api/moderator/events`;
    let busy = false;
    let pending = [];
    const pendingCount = document.getElementById('pendingCount');
    const publishedCount = document.getElementById('publishedCount');
    const publishedNote = document.getElementById('publishedCountNote');
    const pendingNote = document.getElementById('pendingCountNote');
    const totalCount = document.getElementById('totalCount');
    const totalNote = document.getElementById('totalCountNote');

    function node(tag, className, text) {
        const element = document.createElement(tag);
        element.className = className;
        if (text != null) element.textContent = text;
        return element;
    }
    async function request(path, options = {}) {
        const response = await fetch(path.startsWith('http') ? path : base + path, {
            ...options, credentials: 'include', headers: { Accept: 'application/json' }
        });
        const data = await response.json().catch(() => ({}));
        if (!response.ok) {
            const error = new Error(response.status === 401 || response.status === 403
                ? 'Accès réservé aux modérateurs connectés. Vérifie ta session.'
                : data.message || 'Le serveur ne peut pas traiter la demande. Réessaie.');
            error.status = response.status;
            throw error;
        }
        return data;
    }
    function errorText(error) {
        return error instanceof TypeError ? 'Le serveur est inaccessible. Vérifie ta connexion et le démarrage de Spring Boot.' : error.message;
    }
    function render() {
        list.replaceChildren(...pending.map(({ event, creator }) => {
            const card = node('article', 'event-card moderation-card');
            const content = node('div', 'event-card-content');
            const date = new Date(`${event.eventDate}T12:00:00`);
            const dateLabel = Number.isNaN(date.getTime()) ? event.eventDate
                : new Intl.DateTimeFormat('fr-FR', { dateStyle: 'long' }).format(date);
            content.append(node('span', 'event-status status-pending', 'En attente de validation'),
                node('h3', '', event.title), node('p', 'event-description', event.description || ''));
            const details = node('dl', 'moderation-meta');
            for (const [label, value] of [
                ['Date et heure', `${dateLabel} · ${event.eventTime.slice(0, 5)}`],
                ['Lieu', event.location], ['Capacité', `${event.capacity} participants`],
                ['Catégorie', event.categoryName]
            ]) {
                const group = node('div', '');
                group.append(node('dt', '', label), node('dd', '', value));
                details.append(group);
            }
            content.append(details, node('p', 'moderation-creator',
                `Proposé par ${creator.firstName} ${creator.lastName} · Compte n° ${creator.idUser}`));
            const actions = node('div', 'event-card-footer');
            for (const [action, label] of [['approve', 'Accepter'], ['reject', 'Refuser']]) {
                const button = node('button', `secondary-button ${action === 'approve' ? 'approve-button' : 'reject-button'}`, label);
                button.type = 'button';
                button.addEventListener('click', () => decide(event, action));
                actions.append(button);
            }
            content.append(actions);
            card.append(content);
            return card;
        }));
    }
    function lock(value) {
        busy = value;
        refresh.disabled = value;
        list.setAttribute('aria-busy', String(value));
        list.querySelectorAll('button').forEach(button => { button.disabled = value; });
    }
    async function fetchPending() {
        const data = await request('/pending');
        if (!Array.isArray(data)) throw new Error('Réponse du serveur invalide.');
        pending = data;
        render();
    }
    async function loadStats() {
        try {
            const data = await request('/stats');
            if (!['pending', 'published', 'rejected', 'total'].every(key =>
                Number.isSafeInteger(data[key]) && data[key] >= 0)) throw new Error('Réponse invalide');
            if (pendingCount) pendingCount.textContent = String(data.pending);
            if (publishedCount) publishedCount.textContent = String(data.published);
            if (totalCount) totalCount.textContent = String(data.total);
            if (pendingNote) pendingNote.textContent = 'Propositions à examiner';
            if (publishedNote) publishedNote.textContent = 'Visibles dans le catalogue étudiant';
            if (totalNote) totalNote.textContent = `Tous statuts confondus · ${data.rejected} refusé${data.rejected > 1 ? 's' : ''}`;
        } catch {
            for (const counter of [pendingCount, publishedCount, totalCount]) {
                if (counter) counter.textContent = '—';
            }
            for (const note of [pendingNote, publishedNote, totalNote]) {
                if (note) note.textContent = 'Statistiques indisponibles. Actualise la liste pour réessayer.';
            }
        }
    }
    async function load() {
        if (busy) return;
        lock(true);
        message.textContent = 'Chargement des événements...';
        try {
            await fetchPending();
            message.textContent = pending.length ? '' : 'Aucun événement en attente de validation.';
        } catch (error) {
            message.textContent = errorText(error);
        }
        finally { lock(false); }
    }
    async function decide(event, action) {
        if (busy) return;
        const verb = action === 'approve' ? 'Accepter et publier' : 'Refuser';
        if (!window.confirm(`${verb} l’événement « ${event.title} » ? Cette décision ne pourra pas être répétée.`)) return;
        lock(true);
        message.textContent = 'Décision en cours...';
        try {
            const result = await request(`/${event.idEvent}/${action}`, { method: 'PATCH' });
            loadStats();
            pending = pending.filter(item => item.event.idEvent !== event.idEvent);
            render();
            message.textContent = result.message + (pending.length ? '' : ' Aucun événement en attente de validation.');
            try { await fetchPending(); }
            catch { message.textContent += ' Actualisation impossible : utilise « Actualiser la liste ».'; }
        } catch (error) {
            let text = errorText(error);
            if (error.status === 409 || error.status === 404) {
                loadStats();
                try { await fetchPending(); }
                catch { text += ' Actualisation impossible. Réessaie.'; }
            }
            message.textContent = text;
        } finally { lock(false); }
    }
    refresh.addEventListener('click', () => { if (!busy) { load(); loadStats(); } });
    load();
    loadStats();
})();
