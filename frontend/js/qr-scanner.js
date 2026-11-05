(() => {
    const section = document.getElementById('scanner');
    if (!section) return;
    const api = `${window.location.protocol === 'https:' ? window.location.origin : 'http://' + window.location.hostname + ':8080'}/api/check-in`;
    const eventSelect = document.getElementById('scanEvent');
    const status = document.getElementById('scanStatus');
    const result = document.getElementById('scanResult');
    const video = document.getElementById('scanVideo');
    const start = document.getElementById('scanStart');
    const stop = document.getElementById('scanStop');
    const capture = document.getElementById('scanCapture');
    const confirm = document.getElementById('scanConfirm');
    const image = document.getElementById('scanImage');
    const form = document.getElementById('scanImageForm');
    const refresh = document.getElementById('scanRefresh');
    let stream = null, timer = null, pending = null, busy = false, generation = 0, captureNeeded = false;
    const say = (message, state = 'info') => { status.textContent = message; status.dataset.state = state; };
    function controls() {
        start.disabled = busy || !eventSelect.value || !!stream;
        stop.hidden = !stream;
        capture.hidden = !stream || !captureNeeded;
        capture.disabled = busy;
        confirm.disabled = busy || !pending || !eventSelect.value;
        form.querySelector('button').disabled = busy || !eventSelect.value;
        eventSelect.disabled = busy;
        refresh.disabled = busy;
    }
    function stopCamera() {
        generation++;
        clearTimeout(timer); timer = null;
        stream?.getTracks().forEach(track => track.stop());
        stream = null; video.srcObject = null; video.hidden = true;
        controls();
    }
    function reset() { pending = null; result.textContent = ''; stopCamera(); }
    async function request(path, options = {}) {
        const response = await fetch(api + path, {
            ...options, credentials: 'include', cache: 'no-store',
            headers: { Accept: 'application/json', ...options.headers }
        });
        const data = await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(data.message || (response.status === 401 ? 'Session expirée. Reconnectez-vous.'
            : response.status === 403 ? 'Le scan est réservé aux ADMIN et MODERATOR connectés.'
                : response.status === 413 ? 'Image trop volumineuse. Choisissez une image de moins de 5 Mo.' : 'Scan indisponible. Réessayez.'));
        return data;
    }
    async function loadEvents() {
        reset(); say('Chargement des événements du jour…');
        busy = true; controls();
        try {
            const events = await request('/events');
            if (!Array.isArray(events)) throw new Error('Liste des événements indisponible.');
            eventSelect.replaceChildren(new Option('Choisir un événement', ''));
            events.forEach(event => eventSelect.add(new Option(event.title, String(event.idEvent))));
            say(events.length ? 'Choisissez l’événement, puis scannez un billet. La validation enregistre la présence.'
                : 'Aucun événement publié aujourd’hui (heure de Paris).');
        } catch (error) { say(error instanceof TypeError ? 'Serveur inaccessible. Réessayez.' : error.message, 'error'); }
        finally { busy = false; controls(); }
    }
    async function submit(path, body, headers = {}) {
        if (busy || !eventSelect.value) return;
        busy = true; stopCamera(); controls();
        say('Vérification du billet et enregistrement de la présence…'); result.textContent = '';
        try {
            const data = await request(path, { method: 'POST', body, headers: { 'X-EventCampus-Scan': '1', ...headers } });
            say(data.message || 'Réponse de validation reçue.', data.code === 'VALID' ? 'success' : 'used');
            result.textContent = `${data.student || ''}${data.scannedAt ? ' · ' + data.scannedAt.replace('T', ' ') + ' (Paris)' : ''}`;
            result.dataset.state = data.code === 'VALID' ? 'valid' : 'used';
        } catch (error) {
            say(error instanceof TypeError ? 'Connexion interrompue. Réessayez le même billet : la présence ne sera pas comptée deux fois.' : error.message, 'error');
        } finally { busy = false; pending = null; image.value = ''; controls(); }
    }
    async function beginCamera() {
        if (busy || stream || !eventSelect.value) return;
        reset();
        if (!window.isSecureContext || !navigator.mediaDevices?.getUserMedia) {
            say('La caméra nécessite HTTPS sur téléphone (ou localhost sur ordinateur). Utilisez « Autre méthode de scan » pour importer une photo.', 'error'); return;
        }
        const run = ++generation;
        start.disabled = true;
        try {
            const opened = await navigator.mediaDevices.getUserMedia({ video: { facingMode: { ideal: 'environment' } }, audio: false });
            if (run !== generation) { opened.getTracks().forEach(track => track.stop()); return; }
            stream = opened; captureNeeded = false; video.srcObject = opened; video.hidden = false;
            await video.play(); controls();
            let detector = null;
            if ('BarcodeDetector' in window) {
                try {
                    const formats = await window.BarcodeDetector.getSupportedFormats();
                    if (formats.includes('qr_code')) detector = new window.BarcodeDetector({ formats: ['qr_code'] });
                } catch { /* Photo decoding remains available on every supported camera browser. */ }
            }
            captureNeeded = !detector; controls();
            say(detector ? 'Présentez un QR code, puis cliquez sur « Valider la présence ».' : 'Cadrez le QR code puis utilisez « Photographier et valider ».');
            async function detect() {
                if (!stream || run !== generation || !detector) return;
                try {
                    const found = await detector.detect(video);
                    if (!stream || run !== generation) return;
                    const token = found.find(code => /^EC1\.[A-Za-z0-9_-]{43}$/.test(code.rawValue))?.rawValue;
                    if (token) { pending = token; stopCamera(); say('QR code détecté. Confirmez pour vérifier le billet et enregistrer la présence.'); controls(); return; }
                } catch { captureNeeded = true; controls(); say('Lecture automatique indisponible. Utilisez « Photographier et valider ».'); return; }
                timer = setTimeout(detect, 300);
            }
            if (run === generation) detect();
        } catch (error) {
            if (run !== generation) return;
            stopCamera();
            say(error.name === 'NotAllowedError' ? 'Accès caméra refusé. Autorisez-le dans le navigateur ou utilisez « Autre méthode de scan ».' : 'Caméra indisponible. Utilisez « Autre méthode de scan » pour importer une photo.', 'error');
        } finally { controls(); }
    }
    start.addEventListener('click', beginCamera);
    stop.addEventListener('click', () => { stopCamera(); say('Caméra arrêtée.'); });
    confirm.addEventListener('click', () => {
        if (pending) submit('/scan', JSON.stringify({ eventId: Number(eventSelect.value), token: pending }), { 'Content-Type': 'application/json' });
    });
    capture.addEventListener('click', () => {
        if (!stream || !video.videoWidth || busy) return;
        const canvas = document.createElement('canvas');
        const scale = Math.min(1, 1600 / Math.max(video.videoWidth, video.videoHeight));
        canvas.width = Math.round(video.videoWidth * scale); canvas.height = Math.round(video.videoHeight * scale);
        canvas.getContext('2d').drawImage(video, 0, 0, canvas.width, canvas.height);
        canvas.toBlob(blob => {
            if (!blob || !stream) return;
            const body = new FormData(); body.append('eventId', eventSelect.value); body.append('image', blob, 'capture.jpg');
            submit('/scan-image', body);
        }, 'image/jpeg', 0.9);
    });
    form.addEventListener('submit', event => {
        event.preventDefault(); const file = image.files[0];
        if (!file) { say('Choisissez une photo du QR code.', 'error'); return; }
        if (file.size > 5 * 1024 * 1024) { say('Image trop volumineuse (5 Mo maximum).', 'error'); return; }
        const body = new FormData(); body.append('eventId', eventSelect.value); body.append('image', file);
        submit('/scan-image', body);
    });
    eventSelect.addEventListener('change', () => { reset(); controls(); say('Événement sélectionné. Scannez un billet pour vérifier sa validité.'); });
    refresh.addEventListener('click', loadEvents);
    document.getElementById('logoutButton')?.addEventListener('click', reset);
    document.addEventListener('visibilitychange', () => { if (document.hidden) stopCamera(); });
    window.addEventListener('pagehide', reset);
    window.addEventListener('hashchange', () => { if (window.location.hash !== '#scanner') reset(); });
    document.addEventListener('keydown', event => { if (event.key === 'Escape') stopCamera(); });
    loadEvents();
})();
