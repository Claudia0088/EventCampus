const { test } = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
const path = require('node:path');
const source = fs.readFileSync(path.join(__dirname,'../js/qr-code.js'),'utf8');
class Element extends EventTarget {
    constructor() { super(); this.hidden = false; this.open = false; this.isConnected = true; this.attributes = {}; }
    setAttribute(key,value) { this.attributes[key] = value; }
    removeAttribute(key) { delete this.attributes[key]; delete this[key]; }
    showModal() { this.open = true; }
    close() { this.open = false; this.dispatchEvent(new Event('close')); }
    focus() { this.focused = true; }
}
function fixture(options = {}) {
    const ids = ['registrationQrDialog','registrationQrImage','registrationQrDownload','registrationQrStatus','registrationQrClose','registrationQrEvent','registrationQrDate','registrationsMessage'];
    const nodes = Object.fromEntries(ids.map(id => [id, new Element()]));
    const document = new EventTarget(); document.getElementById = id => nodes[id];
    const window = { location: { protocol: options.https ? 'https:' : 'http:', origin:'https://campus.example', hostname:'localhost' } };
    const calls = [], revoked = [];
    vm.runInNewContext(source, {
        document, window, AbortController, TypeError,
        URL: { createObjectURL: () => 'blob:fixture', revokeObjectURL: url => revoked.push(url) },
        fetch: async (url,request) => {
            calls.push({url,request});
            return { ok: !options.status, status: options.status || 200,
                json: async () => options.message ? { message: options.message } : {},
                headers: { get: () => options.badType ? 'text/html' : 'image/png' }, blob: async () => new Blob(['fake-png']) };
        }
    });
    const button = new Element();
    return { nodes, calls, revoked, button, open: () => window.EventCampusQr.open({idRegistration:20,event:{title:'Demo',eventDate:'2026-11-05',eventTime:'14:00:00'}},button) };
}
test('private QR is loaded by POST with cookies and no-store, HTTPS uses same origin', async () => {
    const f = fixture({https:true}); await f.open();
    assert.equal(f.calls[0].url,'https://campus.example/api/user/registrations/20/qr-code');
    assert.equal(f.calls[0].request.method,'POST'); assert.equal(f.calls[0].request.credentials,'include');
    assert.equal(f.calls[0].request.cache,'no-store'); assert.equal(f.nodes.registrationQrImage.hidden,false);
    assert.equal(f.nodes.registrationQrImage.src,'blob:fixture');
});
test('closing QR revokes the image, aborts the request and restores focus', async () => {
    const f = fixture(); await f.open(); f.nodes.registrationQrDialog.close();
    assert.deepEqual(f.revoked,['blob:fixture']); assert.equal(f.calls[0].request.signal.aborted,true);
    assert.equal(f.nodes.registrationQrImage.hidden,true); assert.equal(f.button.focused,true);
});
test('missing session provides reconnect guidance', async () => {
    const f = fixture({status:401}); await f.open();
    assert.match(f.nodes.registrationQrStatus.textContent,/Reconnectez/); assert.equal(f.nodes.registrationQrImage.hidden,true);
});
test('403 provides ownership and student-role guidance', async () => {
    const f = fixture({status:403}); await f.open();
    assert.match(f.nodes.registrationQrStatus.textContent,/compte étudiant propriétaire/);
});
test('503 preserves the server key error without disguising it as 403', async () => {
    const f = fixture({status:503,message:'Clé QR serveur indisponible. Contactez l’administration.'}); await f.open();
    assert.match(f.nodes.registrationQrStatus.textContent,/Clé QR serveur indisponible/);
    assert.equal(f.nodes.registrationQrDownload.hidden,true);
});
test('non-PNG success response never becomes a QR image', async () => {
    const f = fixture({badType:true}); await f.open();
    assert.match(f.nodes.registrationQrStatus.textContent,/Réponse QR invalide/);
    assert.equal(f.nodes.registrationQrImage.hidden,true);
});
