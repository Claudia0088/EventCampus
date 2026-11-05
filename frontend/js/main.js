/* Homepage previews share one request to the existing public catalogue. */
(() => {
    const grid = document.getElementById('homeEvents');
    const board = document.getElementById('heroEvents');
    const message = document.getElementById('homeEventsMessage');
    if (!grid || !board || !message) return;
    const year = document.getElementById('homeYear');
    if (year) year.textContent = String(new Date().getFullYear());

    function node(tag, className, text) {
        const element = document.createElement(tag);
        element.className = className;
        if (text != null) element.textContent = text;
        return element;
    }
    function dateParts(event) {
        const date = new Date(`${event.eventDate}T12:00:00`);
        if (Number.isNaN(date.getTime())) return { day: '—', month: '', full: event.eventDate };
        return {
            day: new Intl.DateTimeFormat('fr-FR', { day: '2-digit' }).format(date),
            month: new Intl.DateTimeFormat('fr-FR', { month: 'short' }).format(date),
            full: new Intl.DateTimeFormat('fr-FR', { dateStyle: 'long' }).format(date)
        };
    }
    function mini(event) {
        const date = dateParts(event);
        const card = node('article', 'hero-mini');
        const calendar = node('div', 'mini-date');
        calendar.append(node('strong', '', date.day), node('span', '', date.month));
        const info = node('div', 'mini-info');
        info.append(node('small', '', event.categoryName || 'Événement'),
            node('h3', '', event.title), node('p', '', `${event.location} · ${String(event.eventTime).slice(0, 5)}`));
        card.append(calendar, info);
        return card;
    }
    function card(event) {
        const article = node('article', 'home-event-card');
        const visual = node('div', 'home-event-visual');
        const symbol = node('span', 'home-event-symbol', '✦');
        symbol.setAttribute('aria-hidden', 'true');
        visual.append(symbol);
        if (event.image) {
            try {
                const url = window.EventCampusImages.resolve(event.image);
                if (['http:', 'https:'].includes(url.protocol)) {
                    const image = node('img', '');
                    image.alt = event.title;
                    image.loading = 'lazy';
                    image.addEventListener('error', () => image.remove(), { once: true });
                    image.src = url.href;
                    visual.append(image);
                }
            } catch { /* Keep the decorative fallback for invalid image URLs. */ }
        }
        visual.append(node('span', 'home-category', event.categoryName || 'Événement'));
        const content = node('div', 'home-event-body');
        content.append(node('p', 'home-event-date', `${dateParts(event).full} · ${String(event.eventTime).slice(0, 5)}`),
            node('h3', '', event.title), node('p', 'home-event-description', event.description || ''),
            node('p', 'home-event-location', `⌖ ${event.location}`));
        article.append(visual, content);
        return article;
    }
    async function load() {
        try {
            const response = await fetch(`http://${window.location.hostname}:8080/api/events`, {
                credentials: 'include', headers: { Accept: 'application/json' }
            });
            if (!response.ok) throw new Error('Le programme est momentanément indisponible. Réessayez dans quelques instants.');
            const data = await response.json();
            if (!Array.isArray(data)) throw new Error('Le programme est momentanément indisponible.');
            const today = new Intl.DateTimeFormat('en-CA', {
                timeZone: 'Europe/Paris', year: 'numeric', month: '2-digit', day: '2-digit'
            }).format(new Date());
            const events = data.filter(event => event.status === 'PUBLIE' && event.eventDate >= today)
                .sort((a, b) => `${a.eventDate}T${a.eventTime}`.localeCompare(`${b.eventDate}T${b.eventTime}`))
                .slice(0, 3);
            grid.replaceChildren(...events.map(card));
            board.replaceChildren(...events.map(mini));
            message.hidden = events.length > 0;
            message.textContent = events.length ? '' : 'Aucun événement à venir pour le moment. Revenez bientôt découvrir le programme.';
            if (!events.length) board.append(node('p', 'home-state', 'Le prochain rendez-vous se prépare. Découvrez les initiatives du campus dans le catalogue.'));
        } catch (error) {
            message.hidden = false;
            message.textContent = error instanceof TypeError
                ? 'Impossible de contacter le serveur. Vérifiez votre connexion ou réessayez plus tard.' : error.message;
            board.replaceChildren(node('p', 'home-state', 'Le programme sera bientôt de retour. Retrouvez tous les événements dans le catalogue.'));
        } finally {
            grid.setAttribute('aria-busy', 'false');
            board.setAttribute('aria-busy', 'false');
        }
    }
    load();
})();
