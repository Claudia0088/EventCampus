/* One shared navigation template and one backend-verified session per page. */
(() => {
    const root = new URL('../', document.currentScript.src);
    const api = `${location.protocol === 'https:' ? location.origin : 'http://' + location.hostname + ':8080'}/api/auth`;
    const destinations = { USER: 'espace-user.html', MODERATOR: 'espace-moderator.html', ADMIN: 'espace-admin.html' };
    const host = document.getElementById('siteHeader');
    if (!host) return;
    host.innerHTML = `<header class="site-header">
        <a class="site-brand" data-page="index.html">EventCampus</a>
        <button class="site-menu-toggle" type="button" aria-controls="siteNavigation" aria-expanded="false" aria-label="Ouvrir la navigation">Menu <span aria-hidden="true">☰</span></button>
        <nav id="siteNavigation" class="site-navigation" aria-label="Navigation principale" aria-busy="true">
            <a data-page="index.html">Accueil</a><a data-page="pages/evenements.html">Événements</a>
            <span class="site-session-links" hidden></span>
        </nav>
        <p class="site-header-message" role="status" hidden></p>
    </header>`;
    host.querySelectorAll('[data-page]').forEach(link => {
        link.href = new URL(link.dataset.page, root).href;
        if (new URL(link.href).pathname === location.pathname) link.setAttribute('aria-current', 'page');
    });
    const nav = host.querySelector('nav');
    const toggle = host.querySelector('button');
    const links = host.querySelector('.site-session-links');
    const message = host.querySelector('[role="status"]');
    function close() { toggle.setAttribute('aria-expanded', 'false'); nav.classList.remove('is-open'); }
    toggle.addEventListener('click', () => {
        const open = toggle.getAttribute('aria-expanded') !== 'true';
        toggle.setAttribute('aria-expanded', String(open)); nav.classList.toggle('is-open', open);
    });
    host.addEventListener('keydown', event => { if (event.key === 'Escape') { close(); toggle.focus(); } });
    document.addEventListener('click', event => { if (!host.contains(event.target)) close(); });
    nav.addEventListener('click', event => { if (event.target.closest('a')) close(); });
    const desktop = matchMedia('(min-width: 701px)');
    desktop.addEventListener('change', close);
    function link(label, page, prominent = false) {
        const item = document.createElement('a'); item.textContent = label; item.href = new URL(page, root).href;
        if (prominent) item.className = 'site-action';
        if (new URL(item.href).pathname === location.pathname) item.setAttribute('aria-current', 'page');
        return item;
    }
    function render(user) {
        links.replaceChildren();
        if (user && destinations[user.role]) {
            links.append(link('Mon espace', 'pages/' + destinations[user.role]));
            const button = document.createElement('button'); button.type = 'button'; button.className = 'site-action'; button.textContent = 'Déconnexion';
            button.addEventListener('click', async () => {
                button.disabled = true; message.hidden = true;
                try {
                    const response = await fetch(api + '/logout', { method: 'POST', credentials: 'include', headers: { Accept: 'application/json' } });
                    if (!response.ok && ![401, 403].includes(response.status)) throw new Error();
                    try { localStorage.removeItem('loggedIn'); localStorage.removeItem('userEmail'); } catch { /* Storage is optional. */ }
                    render(null); location.assign(new URL('index.html', root).href);
                } catch {
                    message.textContent = 'Déconnexion non confirmée. Réessayez.'; message.hidden = false; button.disabled = false;
                }
            });
            links.append(button);
        } else links.append(link('Connexion', 'pages/connexion.html'), link('Inscription ↗', 'pages/inscription.html', true));
        links.hidden = false; nav.setAttribute('aria-busy', 'false');
    }
    async function refresh() {
        try {
            const response = await fetch(api + '/me', { credentials: 'include', cache: 'no-store', headers: { Accept: 'application/json' }, signal: AbortSignal.timeout(8000) });
            if ([401, 403].includes(response.status)) { render(null); return { status: response.status, user: null }; }
            if (!response.ok) throw new Error();
            const user = await response.json();
            if (!destinations[user.role]) throw new Error();
            render(user); return { status: 200, user };
        } catch { render(null); return { status: 0, user: null }; }
    }
    window.EventCampusSession = { ready: refresh(), refresh };
    window.addEventListener('pageshow', event => { if (event.persisted) window.EventCampusSession.ready = refresh(); });
})();
