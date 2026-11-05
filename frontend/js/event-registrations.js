(() => {
    const frontend = new URL('../', document.currentScript.src);
    const api = `${window.location.protocol === 'https:' ? window.location.origin : 'http://' + window.location.hostname + ':8080'}/api`;
    let role = null, mine = [], places = new Map(), ready = false, busy = false;
    const list = document.getElementById('registrationsContainer');
    const notice = document.getElementById('registrationsMessage');
    const catalogueNotice = document.getElementById('registrationActionMessage');
    const cancelDialog = document.getElementById('registrationCancelDialog');
    const cancelForm = document.getElementById('registrationCancelForm');
    const keepButton = document.getElementById('keepRegistration');
    const confirmCancelButton = document.getElementById('confirmRegistrationCancel');
    const cancelError = document.getElementById('registrationCancelError');
    const cancelProgress = document.getElementById('registrationCancelProgress');
    let cancellation = null;
    let cancelPending = false;
    function node(tag, text, className = '') {
        const element = document.createElement(tag); element.className = className;
        if (text != null) element.textContent = text; return element;
    }
    function say(text) {
        if (notice) { notice.textContent = text; notice.classList.remove('registration-success'); }
        if (catalogueNotice) catalogueNotice.textContent = text;
    }
    async function request(path, options = {}) {
        const response = await fetch(api + path, { ...options, credentials: 'include', headers: { Accept: 'application/json' } });
        const data = await response.json().catch(() => ({}));
        if (!response.ok) {
            const error = new Error(data.message || (response.status === 401 || response.status === 403
                ? 'Connectez-vous avec un compte étudiant actif.' : 'Opération indisponible. Réessayez.'));
            error.status = response.status; throw error;
        }
        return data;
    }
    function notify() {
        renderMine(); document.dispatchEvent(new Event('eventcampus:registrations'));
    }
    async function refresh() {
        const availability = await request('/events/availability');
        if (!Array.isArray(availability)) throw new Error('Réponse des disponibilités invalide.');
        places = new Map(availability.map(item => [item.idEvent, item]));
        if (role === 'USER') {
            const data = await request('/user/registrations/mine');
            if (!Array.isArray(data)) throw new Error('Réponse des inscriptions invalide.');
            mine = data;
        }
        ready = true; notify();
    }
    function login() {
        if (window.confirm('Connectez-vous avec un compte étudiant pour vous inscrire. Ouvrir la page de connexion ?'))
            window.location.assign(new URL('pages/connexion.html', frontend).href);
    }
    async function change(path, method, feedback = {}) {
        if (busy) return;
        busy = true; notify(); say('Enregistrement en cours...');
        try {
            const result = await request(path, { method });
            feedback.onSuccess?.();
            const successMessage = feedback.successMessage || result.message;
            try { await refresh(); say(successMessage); }
            catch { ready = false; say(`${successMessage} Actualisation impossible : rechargez la page.`); }
            if (feedback.successMessage && notice) notice.classList.add('registration-success');
        } catch (error) {
            if (error.status === 401 || error.status === 403) { role = null; mine = []; }
            try { await refresh(); } catch { ready = false; }
            const text = error instanceof TypeError ? 'Connexion interrompue. Actualisez pour vérifier le résultat avant de réessayer.' : error.message;
            say(text); feedback.onError?.(text);
        } finally { busy = false; notify(); }
    }
    window.EventCampusRegistrations = {
        canViewQr(id) { return mine.some(item => item.idRegistration === id && item.qrEligible); },
        controls(event) {
            const group = node('div', null, 'registration-controls');
            const availability = places.get(event.idEvent);
            const registered = mine.some(item => item.event.idEvent === event.idEvent && item.status === 'REGISTERED');
            const button = node('button', !ready ? 'Disponibilité indisponible' : registered ? 'Inscrit' : !availability?.open ? 'Inscriptions fermées' : availability.remaining === 0 ? 'Complet' : role && role !== 'USER' ? 'Réservé aux étudiants' : "S’inscrire", 'secondary-button');
            button.type = 'button';
            button.disabled = busy || !ready || registered || !availability?.open || availability.remaining === 0 || (role && role !== 'USER');
            if (availability) group.append(node('p', `${availability.remaining} place(s) restante(s) / ${availability.capacity}`, 'section-description'));
            button.addEventListener('click', () => {
                if (!role) { login(); return; }
                change(`/user/events/${event.idEvent}/registrations`, 'POST');
            });
            group.append(button); return group;
        }
    };
    function restoreFocus() {
        const button = list?.querySelector(`[data-cancel-registration="${cancellation?.idRegistration}"]`);
        const target = button || notice;
        if (target) { if (!button) target.setAttribute('tabindex', '-1'); target.focus(); }
    }
    if (cancelDialog) {
        keepButton.addEventListener('click', () => { if (!cancelPending) cancelDialog.close(); });
        cancelDialog.addEventListener('cancel', event => {
            // A request already sent cannot be undone by closing its confirmation.
            if (cancelPending) event.preventDefault();
        });
        cancelDialog.addEventListener('close', () => { if (!cancelPending) restoreFocus(); });
        cancelForm.addEventListener('submit', async event => {
            event.preventDefault();
            if (!cancellation || cancelPending || busy) return;
            cancelPending = true;
            keepButton.disabled = true; confirmCancelButton.disabled = true;
            cancelForm.setAttribute('aria-busy', 'true');
            cancelError.hidden = true; cancelProgress.hidden = false;
            confirmCancelButton.textContent = 'Annulation en cours…';
            try {
                await change(`/user/registrations/${cancellation.idRegistration}/cancel`, 'PATCH', {
                    successMessage: 'Votre inscription a été annulée avec succès.',
                    onSuccess: () => cancelDialog.close(),
                    onError: text => { cancelError.textContent = text; cancelError.hidden = false; }
                });
            } finally {
                cancelPending = false;
                keepButton.disabled = false; confirmCancelButton.disabled = false;
                confirmCancelButton.textContent = "Confirmer l'annulation";
                cancelProgress.hidden = true; cancelForm.setAttribute('aria-busy', 'false');
                if (cancelDialog.open) keepButton.focus(); else restoreFocus();
            }
        });
    }
    function renderMine() {
        if (!list) return;
        list.replaceChildren(...mine.map(item => {
            const event = item.event;
            const card = node('article', null, 'event-card');
            const visual = node('div', null, 'event-card-image event-image-campus');
            const fallback = node('span', '▦', 'event-image-symbol'); fallback.setAttribute('aria-hidden', 'true'); visual.append(fallback);
            if (event.image) {
                try {
                    const image = node('img', null, 'event-cover'); image.src = window.EventCampusImages.resolve(event.image).href;
                    image.alt = event.title; image.loading = 'lazy'; image.addEventListener('error', () => image.remove(), { once: true }); visual.append(image);
                } catch { /* Keep the fallback. */ }
            }
            const content = node('div', null, 'event-card-content');
            content.append(node('span', item.status === 'REGISTERED' ? 'Inscrit' : item.status === 'CANCELLED' ? 'Annulé' : item.status, 'event-status status-upcoming'),
                node('h3', event.title), node('p', `${event.eventDate} · ${event.eventTime.slice(0, 5)}`, 'event-date'), node('p', event.location, 'event-location'));
            const actions = node('div', null, 'event-card-footer');
            const view = node('a', "Consulter l’événement", 'secondary-button'); view.href = new URL(`pages/evenements.html#event-${event.idEvent}`, frontend).href; actions.append(view);
            if (item.qrEligible && window.EventCampusQr) {
                const qr = node('button', 'Voir mon QR code', 'secondary-button'); qr.type = 'button'; qr.disabled = busy;
                qr.setAttribute('data-qr-registration', String(item.idRegistration));
                qr.addEventListener('click', () => { if (!busy) window.EventCampusQr.open(item, qr); });
                actions.append(qr);
            }
            if (item.cancellable) {
                const cancel = node('button', 'Annuler mon inscription', 'secondary-button'); cancel.type = 'button'; cancel.disabled = busy;
                cancel.setAttribute('data-cancel-registration', String(item.idRegistration));
                cancel.addEventListener('click', () => {
                    if (busy || !cancelDialog || cancelDialog.open) return;
                    cancellation = item;
                    document.getElementById('registrationCancelDescription').textContent = `Vous êtes sur le point d'annuler votre inscription à l'événement “${event.title}”. Souhaitez-vous vraiment continuer ?`;
                    cancelError.hidden = true; cancelError.textContent = '';
                    cancelDialog.showModal(); keepButton.focus();
                }); actions.append(cancel);
            }
            content.append(actions); card.append(visual, content); return card;
        }));
    }
    (async () => {
        try {
            try { role = (await request('/auth/me')).role; }
            catch (error) { if (error.status !== 401 && error.status !== 403) throw error; }
            await refresh();
            if (notice) notice.textContent = role === 'USER' ? mine.length ? '' : 'Vous n’avez pas encore d’inscription.' : 'Connectez-vous avec un compte étudiant pour consulter vos inscriptions.';
        } catch (error) { say(error instanceof TypeError ? 'Serveur inaccessible. Rechargez la page pour réessayer.' : error.message); }
    })();
})();
