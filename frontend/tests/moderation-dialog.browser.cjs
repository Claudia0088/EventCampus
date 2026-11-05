// Offline UI checks using Chrome headless: no backend, account or database access.
// Run: node frontend/tests/moderation-dialog.browser.cjs
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { spawnSync } = require('node:child_process');
const root = path.resolve(__dirname, '../..').replace(/\\/g, '/');
const browser = process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe';
const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'campus-moderation-ui-'));
const original = fs.readFileSync(path.join(root, 'frontend/pages/espace-moderator.html'), 'utf8');
const fixtures = [360, 768, 1440].map(width => {
    let html = original.replace(/<script[\s\S]*?<\/script>/g, '');
    const setup = function () {
        window.testCalls = [];
        window.testPending = true;
        window.fetch = async (url, options = {}) => {
            window.testCalls.push({ url, method: options.method || 'GET' });
            if (options.method === 'PATCH') return new Promise(resolve => { window.resolveDecision = resolve; });
            const data = url.endsWith('/stats') ? { pending: 1, published: 0, rejected: 0, total: 1 }
                : window.testPending ? [{ event: { idEvent: 10, title: 'Atelier <img src=x> Java', description: 'Demo', eventDate: '2026-11-05', eventTime: '14:00:00', location: 'Campus', capacity: 30, categoryName: 'Formation' }, creator: { idUser: 1, firstName: 'Demo', lastName: 'Test' } }] : [];
            return { ok: true, json: async () => data };
        };
        window.confirm = window.alert = () => { throw new Error('Native dialog called'); };
    };
    html = html.replace('<head>', `<head><base href="file:///${root}/frontend/pages/"><script>(${setup.toString()})();<\/script><script src="../js/moderator-events.js" defer><\/script>`);
    return { width, html };
});
const run = async function (fixtures) {
    const results = [];
    const tick = () => new Promise(resolve => setTimeout(resolve, 30));
    for (const fixture of fixtures) {
        const frame = document.createElement('iframe');
        frame.style = `width:${fixture.width}px;height:1000px;border:0`;
        document.body.append(frame);
        await new Promise(resolve => { frame.onload = resolve; frame.srcdoc = fixture.html; });
        await tick();
        const d = frame.contentDocument, w = frame.contentWindow;
        const failures = [];
        const check = (condition, message) => { if (!condition) failures.push(message); };
        const byId = id => d.getElementById(id);
        const dialog = byId('moderationConfirmDialog');
        const form = byId('moderationConfirmForm');
        const cancel = byId('moderationConfirmCancel'), confirm = byId('moderationConfirmSubmit');
        const patchCount = () => w.testCalls.filter(call => call.method === 'PATCH').length;
        let trigger = d.querySelector('.approve-button');
        trigger.click();
        check(dialog.open && dialog.matches(':modal'), 'not a modal dialog');
        check(byId('moderationConfirmTitle').textContent === 'Confirmer la publication', 'wrong title');
        check(byId('moderationConfirmEvent').textContent.includes('<img src=x>') && !dialog.querySelector('img'), 'unsafe title rendering');
        check(d.activeElement === cancel, 'initial focus');
        check(patchCount() === 0, 'published before confirmation');
        check(dialog.getBoundingClientRect().width <= fixture.width && dialog.scrollWidth <= dialog.clientWidth, 'dialog overflow');
        cancel.click(); await tick();
        check(!dialog.open && patchCount() === 0 && d.activeElement === trigger, 'cancel/focus restoration: ' + d.activeElement?.tagName + '#' + d.activeElement?.id);
        trigger.click();
        const escape = new w.Event('cancel', { cancelable: true });
        dialog.dispatchEvent(escape);
        if (!escape.defaultPrevented) dialog.close();
        await tick();
        check(!dialog.open && patchCount() === 0 && d.activeElement === trigger, 'escape cancellation: ' + d.activeElement?.tagName + '#' + d.activeElement?.id);
        trigger.click(); confirm.click(); await tick();
        check(confirm.disabled && cancel.disabled && patchCount() === 1, 'submit lock');
        form.dispatchEvent(new w.Event('submit', { bubbles: true, cancelable: true }));
        check(patchCount() === 1, 'duplicate request');
        const whileSaving = new w.Event('cancel', { cancelable: true }); dialog.dispatchEvent(whileSaving);
        check(whileSaving.defaultPrevented && dialog.open, 'submission dismissed');
        w.resolveDecision({ ok: false, status: 500, json: async () => ({ message: 'Erreur simulee' }) });
        await tick();
        check(dialog.open && !confirm.disabled && byId('moderationConfirmStatus').textContent === 'Erreur simulee', 'error and retry');
        cancel.click(); await tick();
        const reject = d.querySelector('.reject-button'); reject.click();
        check(byId('moderationConfirmTitle').textContent === 'Confirmer le refus', 'reject confirmation');
        confirm.click(); await tick();
        check(w.testCalls.filter(call => call.method === 'PATCH').at(-1).url.endsWith('/10/reject'), 'wrong reject route');
        w.testPending = false;
        w.resolveDecision({ ok: true, json: async () => ({ message: 'Evenement refuse.' }) });
        await tick();
        check(!dialog.open && byId('moderationMessage').textContent.includes('Evenement refuse.'), 'success status');
        check(d.activeElement === byId('moderationMessage'), 'focus after removed event: ' + d.activeElement?.tagName + '#' + d.activeElement?.id);
        // Refill simulated events, then verify the approve action as well.
        w.testPending = true; byId('refreshPending').click(); await tick();
        d.querySelector('.approve-button').click(); confirm.click(); await tick();
        check(w.testCalls.filter(call => call.method === 'PATCH').at(-1).url.endsWith('/10/approve'), 'wrong approve route');
        w.testPending = false;
        w.resolveDecision({ ok: true, json: async () => ({ message: 'Evenement publie.' }) });
        await tick();
        check(!dialog.open && byId('moderationMessage').textContent.includes('Evenement publie.'), 'publication success');
        results.push({ width: fixture.width, failures }); frame.remove();
    }
    document.getElementById('results').textContent = JSON.stringify(results);
};
const file = path.join(directory, 'test.html');
fs.writeFileSync(file, '<pre id="results">waiting</pre><script>(' + run.toString() + ')(' + JSON.stringify(fixtures).replace(/<\/script/gi, '<\\/script') + ');<\/script>');
const result = spawnSync(browser, ['--headless', '--no-sandbox', '--disable-gpu', '--disable-software-rasterizer', '--allow-file-access-from-files', '--user-data-dir=' + path.join(directory, 'profile'), '--virtual-time-budget=15000', '--dump-dom', 'file:///' + file.replace(/\\/g, '/')], { encoding: 'utf8', timeout: 30000, maxBuffer: 3e6 });
const report = result.stdout?.match(/<pre id="results">([\s\S]*?)<\/pre>/)?.[1];
if (!report || report === 'waiting') { console.error('UI tests did not complete', result.stderr?.slice(-500)); process.exitCode = 1; }
else { console.log(report); if (JSON.parse(report).some(test => test.failures.length)) process.exitCode = 1; }
