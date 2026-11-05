/* Student catalogue: only API data is rendered; authentication stays in espace.js. */
(() => {
    const container = document.querySelector('[data-live-events]');
    if (!container) return;
    const message = document.getElementById('eventsMessage');
    const search = document.getElementById('eventSearch');
    const category = document.getElementById('eventCategoryFilter');
    const loginPrompt = document.getElementById('eventsLoginPrompt');
    let events = [];
    let loaded = false;

    function element(tag, className, text) {
        const node = document.createElement(tag);
        node.className = className;
        if (text != null) node.textContent = text;
        return node;
    }

    function card(event) {
        const article = element('article', 'event-card');
        article.dataset.eventId = event.idEvent;
        article.id = `event-${event.idEvent}`;
        const name = event.categoryName || 'Événement';
        const theme = name === 'Formation' ? 'workshop' : name === 'Culture' ? 'conference' : 'campus';
        const visual = element('div', `event-card-image event-image-${theme}`);
        const symbol = element('span', 'event-image-symbol', '✦');
        symbol.setAttribute('aria-hidden', 'true');
        visual.append(symbol, element('span', 'event-category', name));
        if (event.image) {
            try {
                const url = window.EventCampusImages.resolve(event.image);
                if (['http:', 'https:'].includes(url.protocol)) {
                    const img = element('img', 'event-cover');
                    img.alt = event.title;
                    img.loading = 'lazy';
                    img.addEventListener('error', () => img.remove(), { once: true });
                    img.src = url.href;
                    visual.prepend(img);
                }
            } catch { /* The existing illustration remains if the URL is invalid. */ }
        }
        const content = element('div', 'event-card-content');
        // Parse the date at local noon to avoid shifting DATE values across time zones.
        const date = new Date(`${event.eventDate}T12:00:00`);
        const day = Number.isNaN(date.getTime()) ? event.eventDate
            : new Intl.DateTimeFormat('fr-FR', { dateStyle: 'long' }).format(date);
        content.append(
            element('p', 'event-date', `▦ ${day} · ${String(event.eventTime || '').slice(0, 5)}`),
            element('h3', '', event.title),
            element('p', 'event-description', event.description || ''),
            element('p', 'event-location', `⌖ ${event.location}`)
        );
        const footer = element('div', 'event-card-footer');
        footer.append(element('span', 'event-status status-upcoming', 'Publié'));
        if (window.EventCampusRegistrations) footer.append(window.EventCampusRegistrations.controls(event));
        if (window.EventCampusReports) {
            footer.classList.add('reportable-event-actions');
            footer.append(window.EventCampusReports.control(event));
        }
        content.append(footer);
        article.append(visual, content);
        return article;
    }

    function render() {
        if (!loaded) return;
        const term = search.value.trim().toLocaleLowerCase('fr');
        const visible = events.filter(event =>
            (!category.value || String(event.idCategory) === category.value) &&
            `${event.title} ${event.description || ''} ${event.location} ${event.categoryName || ''}`
                .toLocaleLowerCase('fr').includes(term));
        container.replaceChildren(...visible.map(card));
        message.textContent = events.length === 0
            ? 'Aucun événement disponible pour le moment.'
            : visible.length === 0 ? 'Aucun événement ne correspond à ta recherche.' : '';
        message.hidden = visible.length > 0;
    }

    search.addEventListener('input', render);
    category.addEventListener('change', render);
    document.addEventListener('eventcampus:registrations', render);

    async function load() {
        try {
            const response = await fetch(`${window.location.protocol === 'https:' ? window.location.origin : 'http://' + window.location.hostname + ':8080'}/api/events`, {
                credentials: 'include', headers: { Accept: 'application/json' }
            });
            if (response.status === 401 || response.status === 403) {
                if (loginPrompt) loginPrompt.hidden = false;
                throw new Error('Ta session a expiré ou ne permet pas cet accès. Reconnecte-toi pour consulter les événements.');
            }
            if (!response.ok) throw new Error('Impossible de charger les événements. Réessaie dans quelques instants.');
            const data = await response.json();
            if (!Array.isArray(data)) throw new Error('La réponse du serveur est invalide. Réessaie dans quelques instants.');
            events = data.filter(event => event.status === 'PUBLIE');
            const categories = new Map(events.map(event => [String(event.idCategory), event.categoryName || 'Événement']));
            category.replaceChildren(new Option('Toutes les catégories', ''));
            for (const [id, name] of categories) category.add(new Option(name, id));
            loaded = true;
            render();
            if (/^#event-\d+$/.test(window.location.hash)) document.getElementById(window.location.hash.slice(1))?.scrollIntoView();
        } catch (error) {
            container.replaceChildren();
            message.hidden = false;
            message.textContent = error instanceof TypeError
                ? 'Impossible de contacter le serveur. Vérifie ta connexion et que Spring Boot est démarré.'
                : error.message;
        } finally {
            container.setAttribute('aria-busy', 'false');
        }
    }
    load();
})();
