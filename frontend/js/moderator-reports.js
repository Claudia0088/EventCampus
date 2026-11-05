(() => {
    const container = document.getElementById('reportsList');
    if (!container) return;
    const api = `${window.location.protocol === 'https:' ? window.location.origin : 'http://' + window.location.hostname + ':8080'}/api/moderator/reports`;
    const statusFilter = document.getElementById('reportsFilter');
    const refresh = document.getElementById('reportsRefresh');
    const previous = document.getElementById('reportsPrevious');
    const next = document.getElementById('reportsNext');
    const pageLabel = document.getElementById('reportsPage');
    const message = document.getElementById('reportsMessage');
    const dialog = document.getElementById('reportDetailDialog');
    const detailTitle = document.getElementById('reportDetailTitle');
    const detailMeta = document.getElementById('reportDetailMeta');
    const explanation = document.getElementById('reportDetailExplanation');
    const detailStatus = document.getElementById('reportDetailStatus');
    const actions = document.getElementById('reportDetailActions');
    const resolve = document.getElementById('reportResolve');
    const reject = document.getElementById('reportReject');
    const close = document.getElementById('reportDetailClose');
    const confirmation = document.getElementById('reportDecisionForm');
    const confirmationTitle = document.getElementById('reportDecisionTitle');
    const confirmationText = document.getElementById('reportDecisionText');
    const confirm = document.getElementById('reportDecisionConfirm');
    const cancel = document.getElementById('reportDecisionCancel');
    const reasons = { INAPPROPRIATE: 'Contenu inapproprié', MISLEADING: 'Informations trompeuses', SPAM: 'Spam', SAFETY: 'Problème de sécurité', OTHER: 'Autre' };
    const statuses = { PENDING: 'En attente', RESOLVED: 'Traité', REJECTED: 'Rejeté' };
    let page = 0, pages = 0, loading = false, saving = false, selected = null, decision = null, trigger = null, detailRun = 0;
    const say = (element, text, state = 'info') => { element.textContent = text; element.dataset.state = state; };
    const date = value => value ? value.replace('T', ' ').slice(0, 19) + ' (Paris)' : '';
    function node(tag, text, className = '') { const n = document.createElement(tag); n.textContent = text; n.className = className; return n; }
    async function request(path, options = {}) {
        const response = await fetch(api + path, { ...options, credentials: 'include', cache: 'no-store',
            headers: { Accept: 'application/json', ...options.headers } });
        const data = await response.json().catch(() => ({}));
        if (!response.ok) {
            const error = new Error(data.message || (response.status === 401 || response.status === 403
                ? 'Accès réservé aux modérateurs connectés. Reconnectez-vous.' : 'Les signalements sont indisponibles. Réessayez.'));
            error.status = response.status; throw error;
        }
        return data;
    }
    function controls() {
        refresh.disabled = loading || saving; statusFilter.disabled = loading || saving;
        previous.disabled = loading || saving || page <= 0;
        next.disabled = loading || saving || page + 1 >= pages;
        close.disabled = saving; confirm.disabled = saving; cancel.disabled = saving;
        resolve.disabled = saving || !selected; reject.disabled = saving || !selected;
        confirmation.setAttribute('aria-busy', String(saving));
    }
    async function load() {
        if (loading) return;
        loading = true; controls(); container.setAttribute('aria-busy', 'true');
        say(message, 'Chargement des signalements…');
        try {
            let data = await request(`?status=${encodeURIComponent(statusFilter.value)}&page=${page}`);
            if (!Array.isArray(data.items) || !Number.isInteger(data.totalPages)) throw new Error('Réponse des signalements invalide.');
            if (!data.items.length && page > 0) {
                page = Math.max(0, data.totalPages - 1);
                data = await request(`?status=${encodeURIComponent(statusFilter.value)}&page=${page}`);
            }
            pages = data.totalPages;
            container.replaceChildren(...data.items.map(item => {
                const card = node('article', '', 'report-card');
                const heading = node('div', '', 'report-card-heading');
                heading.append(node('h3', item.eventTitle), node('span', statuses[item.status] || item.status, 'report-status'));
                card.append(heading, node('p', reasons[item.reason] || item.reason, 'report-reason'), node('p', date(item.createdAt), 'report-date'));
                const button = node('button', 'Consulter les détails', 'secondary-button'); button.type = 'button';
                button.addEventListener('click', () => showDetail(item, button)); card.append(button); return card;
            }));
            say(message, data.items.length ? `${data.totalElements} signalement(s) · ${statuses[statusFilter.value] || 'Tous les statuts'}` : 'Aucun signalement pour ce statut.');
            pageLabel.textContent = pages ? `Page ${page + 1} sur ${pages}` : 'Aucune page';
        } catch (error) {
            container.replaceChildren(); pages = 0; pageLabel.textContent = '';
            say(message, error instanceof TypeError ? 'Serveur inaccessible. Réessayez.' : error.message, 'error');
        } finally { loading = false; container.setAttribute('aria-busy', 'false'); controls(); }
    }
    function restoreFocus() {
        if (dialog.open || !trigger) return;
        detailRun++;
        const target = trigger.isConnected && !trigger.disabled ? trigger : message;
        trigger = null; selected = null; decision = null;
        target.setAttribute('tabindex', '-1'); target.focus();
    }
    function dismiss() { if (!saving) { dialog.close(); restoreFocus(); } }
    function clearDecision() { decision = null; confirmation.hidden = true; actions.hidden = !selected || selected.report.status !== 'PENDING'; }
    async function showDetail(item, button) {
        if (dialog.open || saving) return;
        trigger = button; selected = null; decision = null;
        const run = ++detailRun;
        detailTitle.textContent = item.eventTitle; detailMeta.textContent = ''; explanation.textContent = '';
        confirmation.hidden = true; actions.hidden = true; say(detailStatus, 'Chargement des détails…');
        dialog.showModal(); close.focus(); controls();
        try {
            const data = await request('/' + item.idReport);
            if (run !== detailRun || !dialog.open) return;
            selected = data;
            detailTitle.textContent = data.report.eventTitle;
            detailMeta.textContent = `${reasons[data.report.reason] || data.report.reason} · ${statuses[data.report.status] || data.report.status} · ${date(data.report.createdAt)}`;
            explanation.textContent = data.explanation || 'Aucune explication complémentaire.';
            say(detailStatus, data.processedAt ? `Décision enregistrée le ${date(data.processedAt)}.` : 'Consultez le signalement avant de prendre une décision.');
            clearDecision(); controls();
        } catch (error) {
            if (run === detailRun && dialog.open) say(detailStatus, error instanceof TypeError ? 'Serveur inaccessible.' : error.message, 'error');
        }
    }
    function choose(status) {
        if (saving || !selected || selected.report.status !== 'PENDING') return;
        decision = status; actions.hidden = true; confirmation.hidden = false;
        confirmationTitle.textContent = status === 'RESOLVED' ? 'Marquer comme traité ?' : 'Rejeter ce signalement ?';
        confirmationText.textContent = 'Cette décision est définitive. Elle ne modifie pas la publication de l’événement.';
        confirm.textContent = status === 'RESOLVED' ? 'Confirmer le traitement' : 'Confirmer le rejet'; cancel.focus();
    }
    resolve.addEventListener('click', () => choose('RESOLVED'));
    reject.addEventListener('click', () => choose('REJECTED'));
    cancel.addEventListener('click', () => { if (!saving) { const button = decision === 'RESOLVED' ? resolve : reject; clearDecision(); button.focus(); } });
    close.addEventListener('click', dismiss);
    dialog.addEventListener('cancel', event => { event.preventDefault(); dismiss(); });
    dialog.addEventListener('close', restoreFocus);
    confirmation.addEventListener('submit', async event => {
        event.preventDefault(); if (saving || !selected || !decision || confirmation.hidden) return;
        saving = true; controls(); say(detailStatus, 'Enregistrement de la décision…');
        try {
            const data = await request('/' + selected.report.idReport, { method: 'PATCH',
                headers: { 'Content-Type': 'application/json', 'X-EventCampus-Report': '1' }, body: JSON.stringify({ status: decision }) });
            selected = null;
            await load(); saving = false; dismiss(); say(message, data.message || 'Décision enregistrée.', 'success');
        } catch (error) {
            const text = error instanceof TypeError ? 'Connexion interrompue. Actualisez avant de réessayer.' : error.message;
            if (error.status === 404 || error.status === 409) { selected = null; await load(); saving = false; dismiss(); say(message, text, 'error'); }
            else say(detailStatus, text, 'error');
        } finally { saving = false; controls(); }
    });
    statusFilter.addEventListener('change', () => { page = 0; load(); });
    refresh.addEventListener('click', () => { if (!saving) load(); });
    previous.addEventListener('click', () => { if (!loading && !saving && page > 0) { page--; load(); } });
    next.addEventListener('click', () => { if (!loading && !saving && page + 1 < pages) { page++; load(); } });
    load();
})();
