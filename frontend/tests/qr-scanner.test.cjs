// Run with: node --test frontend/tests/qr-scanner.test.cjs
// No HTTP server, real camera, account, database or external dependencies.
const { test } = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
const path = require('node:path');
const source = fs.readFileSync(path.join(__dirname, '../js/qr-scanner.js'), 'utf8');
const settle = async () => { for (let i = 0; i < 4; i++) await new Promise(resolve => setImmediate(resolve)); };

class Element extends EventTarget {
    constructor() { super(); this.value = ''; this.textContent = ''; this.dataset = {}; this.hidden = false; this.disabled = false; this.files = []; this.options = []; }
    add(option) { this.options.push(option); }
    replaceChildren(...options) { this.options = options; this.value = ''; }
    querySelector() { return this.button; }
    click() { if (!this.disabled) this.dispatchEvent(new Event('click')); }
    async play() {}
}
function fixture(options = {}) {
    const ids = ['scanner','scanEvent','scanStatus','scanResult','scanVideo','scanStart','scanStop','scanCapture','scanConfirm','scanImage','scanImageForm','scanRefresh','logoutButton'];
    const elements = Object.fromEntries(ids.map(id => [id, new Element()]));
    elements.scanImageForm.button = new Element();
    const document = new EventTarget();
    document.getElementById = id => elements[id];
    document.createElement = () => ({ getContext: () => ({ drawImage() {} }), toBlob: callback => callback(new Blob(['photo'], { type: 'image/jpeg' })) });
    elements.scanVideo.videoWidth = 800; elements.scanVideo.videoHeight = 600;
    const window = new EventTarget();
    window.location = { protocol: options.https ? 'https:' : 'http:', hostname: 'localhost', origin: options.https ? 'https://campus.example' : 'http://localhost', hash: '#scanner' };
    window.isSecureContext = !options.insecure;
    const calls = []; let stopped = 0;
    const media = { getTracks: () => [{ stop() { stopped++; } }] };
    const navigator = { mediaDevices: { getUserMedia: async () => {
        if (options.denied) { const error = new Error(); error.name = 'NotAllowedError'; throw error; }
        return media;
    } } };
    if (options.native) window.BarcodeDetector = class {
        static async getSupportedFormats() { return ['qr_code']; }
        async detect() { return [{ rawValue: 'EC1.' + 'A'.repeat(43) }]; }
    };
    const context = {
        window, document, navigator, Option: class { constructor(text,value) { this.text = text; this.value = value; } },
        FormData, Blob, TypeError, setTimeout, clearTimeout, console,
        fetch: async (url, request) => {
            calls.push({ url, request });
            if (url.endsWith('/events')) return { ok: true, json: async () => options.empty ? [] : [{ idEvent: 10, title: 'Demo' }] };
            if (options.networkFailure) throw new TypeError('offline');
            return { ok: true, json: async () => ({ code: options.used ? 'ALREADY_USED' : 'VALID', message: options.used ? 'Presence deja enregistree.' : 'Presence enregistree.', student: 'Camille Demo', scannedAt: '2026-11-05T13:00:00' }) };
        }
    };
    vm.runInNewContext(source, context);
    return { elements, calls, document, window, stopped: () => stopped, async select() { await settle(); elements.scanEvent.value = '10'; elements.scanEvent.dispatchEvent(new Event('change')); } };
}
test('today events are requested with credentials and HTTPS uses the same origin', async () => {
    const f = fixture({ https: true }); await settle();
    assert.equal(f.calls[0].url, 'https://campus.example/api/check-in/events');
    assert.equal(f.calls[0].request.credentials, 'include');
    assert.equal(f.elements.scanEvent.options.length, 2);
    assert.equal(f.elements.scanStart.disabled, true);
});
test('no events leaves scanner disabled', async () => {
    const f = fixture({ empty: true }); await settle();
    assert.equal(f.elements.scanStart.disabled, true);
    assert.match(f.elements.scanStatus.textContent, /Aucun événement/);
});
test('camera permission refusal is explained without submitting a scan', async () => {
    const f = fixture({ denied: true }); await f.select(); f.elements.scanStart.click(); await settle();
    assert.match(f.elements.scanStatus.textContent, /caméra refusé/);
    assert.equal(f.calls.length, 1);
});
test('HTTP network camera is rejected with HTTPS guidance', async () => {
    const f = fixture({ insecure: true }); await f.select(); f.elements.scanStart.click(); await settle();
    assert.match(f.elements.scanStatus.textContent, /HTTPS/);
});
test('without native detector, camera photo fallback is available and tracks stop on hide', async () => {
    const f = fixture(); await f.select(); f.elements.scanStart.click(); await settle();
    assert.equal(f.elements.scanCapture.hidden, false);
    assert.match(f.elements.scanStatus.textContent, /Photographier et valider/);
    f.document.hidden = true; f.document.dispatchEvent(new Event('visibilitychange'));
    assert.equal(f.stopped(), 1); assert.equal(f.elements.scanVideo.srcObject, null);
});
test('native detection requires confirmation and sends only token and chosen event', async () => {
    const f = fixture({ native: true }); await f.select(); f.elements.scanStart.click(); await settle();
    assert.equal(f.calls.length, 1); assert.equal(f.stopped(), 1);
    assert.equal(f.elements.scanConfirm.disabled, false);
    f.elements.scanConfirm.click(); await settle();
    assert.equal(f.calls.length, 2);
    assert.equal(f.calls[1].request.headers['X-EventCampus-Scan'], '1');
    assert.deepEqual(JSON.parse(f.calls[1].request.body), { eventId: 10, token: 'EC1.' + 'A'.repeat(43) });
    assert.equal(f.elements.scanResult.dataset.state, 'valid');
    assert.equal(f.elements.scanStatus.dataset.state, 'success');
    assert.equal(f.elements.scanConfirm.disabled, true);
});
test('used ticket displays already-used result', async () => {
    const f = fixture({ native: true, used: true }); await f.select(); f.elements.scanStart.click(); await settle();
    f.elements.scanConfirm.click(); await settle();
    assert.equal(f.elements.scanResult.dataset.state, 'used');
    assert.equal(f.elements.scanStatus.dataset.state, 'used');
    assert.match(f.elements.scanStatus.textContent, /deja/);
});
test('network failure explains safe retry and reenables scanner', async () => {
    const f = fixture({ native: true, networkFailure: true }); await f.select(); f.elements.scanStart.click(); await settle();
    f.elements.scanConfirm.click(); await settle();
    assert.match(f.elements.scanStatus.textContent, /pas comptée deux fois/);
    assert.equal(f.elements.scanStatus.dataset.state, 'error');
    assert.equal(f.elements.scanStart.disabled, false);
});
test('refresh reloads events and clears event selection', async () => {
    const f = fixture(); await f.select(); f.elements.scanRefresh.click(); await settle();
    assert.equal(f.calls.length, 2); assert.equal(f.calls[1].url.endsWith('/events'), true);
    assert.equal(f.elements.scanEvent.value, ''); assert.equal(f.elements.scanStart.disabled, true);
});
test('photo import still submits the selected event with security header', async () => {
    const f = fixture(); await f.select();
    f.elements.scanImage.files = [new Blob(['photo'], { type: 'image/png' })];
    f.elements.scanImageForm.dispatchEvent(new Event('submit', { cancelable: true })); await settle();
    assert.equal(f.calls[1].url.endsWith('/scan-image'), true);
    assert.equal(f.calls[1].request.body.get('eventId'), '10');
    assert.equal(f.calls[1].request.headers['X-EventCampus-Scan'], '1');
    assert.equal(f.elements.scanStatus.dataset.state, 'success');
});
test('capture fallback validates a photo and stops camera', async () => {
    const f = fixture(); await f.select(); f.elements.scanStart.click(); await settle();
    f.elements.scanCapture.click(); await settle();
    assert.equal(f.calls[1].url.endsWith('/scan-image'), true);
    assert.equal(f.stopped(), 1); assert.equal(f.elements.scanStatus.dataset.state, 'success');
});
