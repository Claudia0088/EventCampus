(() => {
    const dialog = document.getElementById('registrationQrDialog');
    if (!dialog) return;
    const image = document.getElementById('registrationQrImage');
    const download = document.getElementById('registrationQrDownload');
    const status = document.getElementById('registrationQrStatus');
    const close = document.getElementById('registrationQrClose');
    let objectUrl = null, requestNumber = 0, request = null, selected = null, trigger = null;
    function clearImage() {
        image.hidden = true; image.removeAttribute('src');
        download.hidden = true; download.removeAttribute('href');
        if (objectUrl) URL.revokeObjectURL(objectUrl);
        objectUrl = null;
    }
    close.addEventListener('click', () => dialog.close());
    dialog.addEventListener('close', () => {
        requestNumber++; request?.abort(); clearImage(); selected = null;
        const focus = trigger?.isConnected ? trigger : document.getElementById('registrationsMessage');
        if (focus) { focus.setAttribute('tabindex', '-1'); focus.focus(); }
    });
    document.addEventListener('eventcampus:registrations', () => {
        if (dialog.open && selected != null && !window.EventCampusRegistrations?.canViewQr(selected)) dialog.close();
    });
    window.EventCampusQr = {
        async open(item, button) {
            if (dialog.open) return;
            selected = item.idRegistration; trigger = button;
            const generation = ++requestNumber;
            clearImage();
            document.getElementById('registrationQrEvent').textContent = item.event.title;
            document.getElementById('registrationQrDate').textContent = `${item.event.eventDate} · ${item.event.eventTime.slice(0, 5)}`;
            status.textContent = 'Préparation de votre QR code…'; dialog.setAttribute('aria-busy', 'true');
            dialog.showModal(); close.focus(); request = new AbortController();
            try {
                const response = await fetch(`${window.location.protocol === 'https:' ? window.location.origin : 'http://' + window.location.hostname + ':8080'}/api/user/registrations/${item.idRegistration}/qr-code`, {
                    method: 'POST', credentials: 'include', headers: { Accept: 'image/png, application/json' },
                    cache: 'no-store', signal: request.signal
                });
                if (!response.ok) {
                    const error = await response.json().catch(() => ({}));
                    throw new Error(error.message || (response.status === 401
                        ? 'Session absente ou expirée. Reconnectez-vous.' : response.status === 403
                            ? 'Accès refusé : reconnectez-vous avec le compte étudiant propriétaire de cette inscription.'
                            : 'Impossible de charger le QR code. Réessayez.'));
                }
                if (!response.headers.get('Content-Type')?.startsWith('image/png')) throw new Error('Réponse QR invalide. Réessayez.');
                const png = await response.blob();
                if (!dialog.open || generation !== requestNumber) return;
                objectUrl = URL.createObjectURL(png);
                image.src = objectUrl; image.alt = `QR code personnel pour ${item.event.title}`; image.hidden = false;
                download.href = objectUrl; download.download = `EventCampus-QR-${item.idRegistration}.png`; download.hidden = false;
                status.textContent = 'Votre QR code personnel est prêt.';
            } catch (error) {
                if (generation !== requestNumber || error.name === 'AbortError') return;
                clearImage(); status.textContent = error instanceof TypeError ? 'Serveur inaccessible. Fermez la fenêtre et réessayez.' : error.message;
            } finally { if (generation === requestNumber) dialog.setAttribute('aria-busy', 'false'); }
        }
    };
})();
