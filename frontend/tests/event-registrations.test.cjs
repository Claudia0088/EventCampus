const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
class Element {
    constructor() { this.children = []; this.handlers = {}; this.open = false; this.classes = new Set(); this.classList = { add: value => this.classes.add(value), remove: value => this.classes.delete(value) }; }
    append(...nodes) { this.children.push(...nodes); }
    replaceChildren(...nodes) { this.children = nodes; }
    setAttribute(key, value) { this[key] = value; }
    addEventListener(key, handler) { this.handlers[key] = handler; }
    showModal() { this.open = true; }
    close() { this.open = false; this.handlers.close?.(); }
    focus() { this.focused = true; }
    querySelector(selector) {
        const id = selector.match(/="([^"]+)"/)?.[1];
        for (const child of this.children) {
            if (child['data-cancel-registration'] === id) return child;
            const found = child.querySelector(selector); if (found) return found;
        }
        return null;
    }
}
const tick = () => new Promise(resolve => setImmediate(resolve));
async function setup({ visitor = false, enrolled = false, fail = false, delayed = false } = {}) {
    const nodes = Object.fromEntries(['registrationsContainer', 'registrationsMessage', 'registrationActionMessage',
        'registrationCancelDialog', 'registrationCancelForm', 'keepRegistration', 'confirmRegistrationCancel',
        'registrationCancelError', 'registrationCancelProgress', 'registrationCancelDescription'].map(key => [key, new Element()]));
    let confirmed = false, writes = 0, destination;
    let release;
    const event = { idEvent: 10, title: '<b>Event</b>', eventDate: '2099-11-16', eventTime: '14:00:00', location: 'Campus' };
    let current = enrolled;
    const context = {
        URL, Event, Map, Error, TypeError,
        document: { currentScript: { src: 'http://127.0.0.1:5501/frontend/js/event-registrations.js' },
            getElementById: key => nodes[key], createElement: () => new Element(), dispatchEvent() {} },
        window: { location: { hostname: '127.0.0.1', assign: url => { destination = url; } }, confirm: () => confirmed },
        fetch: async (url, options) => {
            assert.equal(options.credentials, 'include');
            if (url.endsWith('/auth/me')) return { ok: !visitor, status: visitor ? 401 : 200, json: async () => ({ role: 'USER' }) };
            if (options.method) {
                writes++;
                if (delayed) await new Promise(resolve => { release = resolve; });
                if (fail) return { ok: false, status: 500, json: async () => ({ message: 'Server error' }) };
                current = options.method === 'POST';
                return { ok: true, json: async () => ({ message: 'Saved' }) };
            }
            if (url.endsWith('/availability')) return { ok: true, json: async () => [{ idEvent: 10, capacity: 1, remaining: current ? 0 : 1, open: true }] };
            return { ok: true, json: async () => current ? [{ idRegistration: 20, status: 'REGISTERED', event, cancellable: true }] : [] };
        }
    };
    vm.runInNewContext(fs.readFileSync('frontend/js/event-registrations.js', 'utf8'), context);
    await tick();
    return { nodes, event, controls: () => context.window.EventCampusRegistrations.controls(event),
        confirm: () => { confirmed = true; }, writes: () => writes, destination: () => destination, release: () => release() };
}
test('visitor is invited to connect without creating a registration', async () => {
    const app = await setup({ visitor: true });
    app.controls().children.at(-1).handlers.click(); assert.equal(app.writes(), 0); assert.equal(app.destination(), undefined);
    app.confirm(); app.controls().children.at(-1).handlers.click();
    assert.equal(app.destination(), 'http://127.0.0.1:5501/frontend/pages/connexion.html'); assert.equal(app.writes(), 0);
});
test('registers through API and refreshes membership and remaining seats', async () => {
    const app = await setup(); app.controls().children.at(-1).handlers.click(); await tick();
    assert.equal(app.writes(), 1); assert.equal(app.controls().children.at(-1).textContent, 'Inscrit');
    assert.match(app.controls().children[0].textContent, /^0 place/); assert.equal(app.nodes.registrationsContainer.children.length, 1);
});
test('cancellation requires confirmation and releases the place', async () => {
    const app = await setup({ enrolled: true });
    const cancel = app.nodes.registrationsContainer.children[0].children[1].children.at(-1).children.at(-1);
    cancel.handlers.click(); assert.equal(app.writes(), 0);
    assert.equal(app.nodes.registrationCancelDialog.open, true);
    assert.equal(app.nodes.keepRegistration.focused, true);
    assert.ok(app.nodes.registrationCancelDescription.textContent.includes('<b>Event</b>'));
    app.nodes.keepRegistration.handlers.click(); assert.equal(app.nodes.registrationCancelDialog.open, false); assert.equal(app.writes(), 0);
    cancel.handlers.click(); await app.nodes.registrationCancelForm.handlers.submit({ preventDefault() {} }); assert.equal(app.writes(), 1);
    assert.equal(app.nodes.registrationCancelDialog.open, false);
    assert.equal(app.nodes.registrationsMessage.textContent, 'Votre inscription a été annulée avec succès.');
    assert.ok(app.nodes.registrationsMessage.classes.has('registration-success'));
    assert.match(app.controls().children[0].textContent, /^1 place/);
});
test('cancellation blocks double submission and closing while the request is pending', async () => {
    const app = await setup({ enrolled: true, delayed: true });
    app.nodes.registrationsContainer.children[0].children[1].children.at(-1).children.at(-1).handlers.click();
    const submit = () => app.nodes.registrationCancelForm.handlers.submit({ preventDefault() {} });
    const first = submit(); await submit(); assert.equal(app.writes(), 1);
    assert.equal(app.nodes.confirmRegistrationCancel.disabled, true);
    assert.equal(app.nodes.registrationCancelProgress.hidden, false);
    let prevented = false;
    app.nodes.registrationCancelDialog.handlers.cancel({ preventDefault() { prevented = true; } });
    assert.equal(prevented, true);
    app.release(); await first;
    assert.equal(app.nodes.confirmRegistrationCancel.disabled, false);
    assert.equal(app.nodes.registrationCancelDialog.open, false);
});
test('cancellation server error remains in modal and does not announce success', async () => {
    const app = await setup({ enrolled: true, fail: true });
    app.nodes.registrationsContainer.children[0].children[1].children.at(-1).children.at(-1).handlers.click();
    await app.nodes.registrationCancelForm.handlers.submit({ preventDefault() {} });
    assert.equal(app.nodes.registrationCancelDialog.open, true);
    assert.equal(app.nodes.registrationCancelError.textContent, 'Server error');
    assert.equal(app.nodes.registrationCancelError.hidden, false);
    assert.equal(app.nodes.registrationsMessage.classes.has('registration-success'), false);
    assert.equal(app.nodes.keepRegistration.disabled, false);
});
test('Escape can close an idle dialog without any API mutation', async () => {
    const app = await setup({ enrolled: true });
    app.nodes.registrationsContainer.children[0].children[1].children.at(-1).children.at(-1).handlers.click();
    let prevented = false;
    app.nodes.registrationCancelDialog.handlers.cancel({ preventDefault() { prevented = true; } });
    if (!prevented) app.nodes.registrationCancelDialog.close();
    assert.equal(app.nodes.registrationCancelDialog.open, false);
    assert.equal(app.writes(), 0);
});
test('server refusal never creates frontend membership', async () => {
    const app = await setup({ fail: true }); app.controls().children.at(-1).handlers.click(); await tick();
    assert.equal(app.writes(), 1); assert.equal(app.nodes.registrationsContainer.children.length, 0);
    assert.equal(app.nodes.registrationActionMessage.textContent, 'Server error');
});
