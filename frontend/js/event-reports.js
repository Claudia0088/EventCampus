(() => {
    const dialog = document.getElementById('eventReportDialog');
    if (!dialog) return;
    const form = document.getElementById('eventReportForm');
    const fields = document.getElementById('eventReportFields');
    const title = document.getElementById('eventReportEvent');
    const reason = document.getElementById('eventReportReason');
    const explanation = document.getElementById('eventReportExplanation');
    const count = document.getElementById('eventReportCount');
    const status = document.getElementById('eventReportStatus');
    const send = document.getElementById('eventReportSend');
    const close = document.getElementById('eventReportClose');
    const api = `${window.location.protocol === 'https:' ? window.location.origin : 'http://' + window.location.hostname + ':8080'}/api/user/events`;
    const reported = new Set();
    let selected = null, trigger = null, saving = false;
    function say(message, state = 'info') { status.textContent = message; status.dataset.state = state; }
    function buttons() {
        document.querySelectorAll('[data-report-event]').forEach(button => {
            const done = reported.has(Number(button.dataset.reportEvent));
            button.disabled = saving || done;
            button.textContent = done ? 'Signalement envoyé' : 'Signaler';
        });
    }
    function restoreFocus() {
        if (dialog.open || !trigger) return;
        const target = trigger.isConnected && !trigger.disabled ? trigger : document.getElementById('evenements');
        trigger = null; selected = null;
        if (target) { target.setAttribute('tabindex', '-1'); target.focus(); }
    }
    function dismiss() { if (!saving) { dialog.close(); restoreFocus(); } }
    function updateFields() {
        explanation.required = reason.value === 'OTHER';
        count.textContent = `${explanation.value.length} / 2 000 caractères${explanation.required ? ' · Explication obligatoire' : ''}`;
    }
    function open(event, button) {
        if (saving || dialog.open || reported.has(event.idEvent)) return;
        selected = event; trigger = button;
        title.textContent = event.title;
        form.reset(); fields.hidden = false; send.hidden = false; close.textContent = 'Annuler';
        say(''); updateFields(); dialog.showModal(); reason.focus();
    }
    window.EventCampusReports = {
        control(event) {
            const button = document.createElement('button');
            button.type = 'button'; button.className = 'secondary-button report-event-button';
            button.dataset.reportEvent = String(event.idEvent);
            button.textContent = reported.has(event.idEvent) ? 'Signalement envoyé' : 'Signaler';
            button.disabled = saving || reported.has(event.idEvent);
            button.setAttribute('aria-label', `Signaler l'événement ${event.title}`);
            button.addEventListener('click', () => open(event, button));
            return button;
        }
    };
    reason.addEventListener('change', updateFields);
    explanation.addEventListener('input', updateFields);
    close.addEventListener('click', dismiss);
    dialog.addEventListener('cancel', event => { event.preventDefault(); dismiss(); });
    dialog.addEventListener('close', restoreFocus);
    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (saving || !selected || !dialog.open || !form.reportValidity()) return;
        saving = true; send.disabled = true; close.disabled = true;
        fields.disabled = true; form.setAttribute('aria-busy', 'true'); buttons();
        say('Envoi du signalement…');
        try {
            const response = await fetch(`${api}/${selected.idEvent}/reports`, {
                method: 'POST', credentials: 'include', cache: 'no-store',
                headers: { Accept: 'application/json', 'Content-Type': 'application/json', 'X-EventCampus-Report': '1' },
                body: JSON.stringify({ reason: reason.value, explanation: explanation.value })
            });
            const data = await response.json().catch(() => ({}));
            if (!response.ok) throw new Error(data.message || (response.status === 401 || response.status === 403
                ? 'Reconnectez-vous avec un compte étudiant actif pour signaler un événement.' : 'Envoi indisponible. Réessayez plus tard.'));
            reported.add(selected.idEvent);
            say(data.message || 'Merci, votre signalement a été transmis.', 'success');
            fields.hidden = true; send.hidden = true; close.textContent = 'Fermer';
        } catch (error) {
            say(error instanceof TypeError ? 'Connexion interrompue. Vous pouvez réessayer : un seul signalement sera enregistré pour cet événement.' : error.message, 'error');
        } finally {
            saving = false; send.disabled = false; close.disabled = false; fields.disabled = false;
            form.setAttribute('aria-busy', 'false'); buttons();
            if (send.hidden) close.focus();
        }
    });
})();
