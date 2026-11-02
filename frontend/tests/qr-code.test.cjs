const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
class Element {
    constructor() { this.handlers = {}; this.isConnected = true; }
    addEventListener(name, action) { this.handlers[name] = action; }
    setAttribute(name, value) { this[name] = value; }
    removeAttribute(name) { delete this[name]; }
    showModal() { this.open = true; }
    close() { this.open = false; this.handlers.close?.(); }
    focus() { this.focused = true; }
}
function setup({ failure = false, delayed = false } = {}) {
    const ids = ['registrationQrDialog','registrationQrImage','registrationQrDownload','registrationQrStatus','registrationQrClose','registrationQrEvent','registrationQrDate','registrationsMessage'];
    const nodes = Object.fromEntries(ids.map(id => [id,new Element()]));
    let created = 0, revoked = 0, release, eligible = true;
    const handlers = {};
    const context = {
        Error, TypeError, AbortController,
        URL: { createObjectURL() { created++; return 'blob:private-qr'; }, revokeObjectURL() { revoked++; } },
        document: { getElementById: id => nodes[id], addEventListener: (name, handler) => { handlers[name] = handler; } },
        window: { location: { hostname: '127.0.0.1' }, EventCampusRegistrations: { canViewQr: () => eligible } },
        fetch: async (url, options) => {
            assert.ok(url.endsWith('/20/qr-code')); assert.equal(options.method,'POST');
            assert.equal(options.credentials,'include'); assert.equal(options.cache,'no-store');
            if (delayed) await new Promise(resolve => { release = resolve; });
            return { ok: !failure, headers: { get: () => 'image/png' },
                blob: async () => new Blob(['mock image'],{type:'image/png'}), json: async () => ({message:'Ticket unavailable'}) };
        }
    };
    vm.runInNewContext(fs.readFileSync('frontend/js/qr-code.js','utf8'),context);
    const item = { idRegistration:20,event:{title:'<b>Event</b>',eventDate:'2099-11-16',eventTime:'14:00:00'} };
    return { nodes, open: () => context.window.EventCampusQr.open(item,new Element()), release: () => release(),
        created: () => created, revoked: () => revoked, invalidate: () => { eligible=false;handlers['eventcampus:registrations'](); } };
}
test('QR modal displays event text and downloads the same private PNG',async()=>{
    const app=setup();await app.open();assert.equal(app.nodes.registrationQrDialog.open,true);
    assert.equal(app.nodes.registrationQrEvent.textContent,'<b>Event</b>');assert.equal(app.nodes.registrationQrImage.hidden,false);
    assert.equal(app.nodes.registrationQrDownload.href,'blob:private-qr');assert.match(app.nodes.registrationQrDownload.download,/\.png$/);
    app.nodes.registrationQrClose.handlers.click();assert.equal(app.revoked(),1);assert.equal(app.nodes.registrationQrDownload.hidden,true);
});
test('server errors keep image and download unavailable',async()=>{
    const app=setup({failure:true});await app.open();assert.equal(app.nodes.registrationQrImage.hidden,true);
    assert.equal(app.nodes.registrationQrDownload.hidden,true);assert.equal(app.nodes.registrationQrStatus.textContent,'Ticket unavailable');assert.equal(app.created(),0);
});
test('closing while loading never reveals a late image',async()=>{
    const app=setup({delayed:true});const pending=app.open();app.nodes.registrationQrClose.handlers.click();app.release();await pending;
    assert.equal(app.created(),0);assert.equal(app.nodes.registrationQrImage.hidden,true);
});
test('cancellation update closes modal and revokes download URL',async()=>{
    const app=setup();await app.open();app.invalidate();assert.equal(app.nodes.registrationQrDialog.open,false);
    assert.equal(app.revoked(),1);assert.equal(app.nodes.registrationQrDownload.hidden,true);
});
