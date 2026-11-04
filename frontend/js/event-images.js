/* Canonical database paths (/frontend/...) work with Live Server and WAMP. */
(() => {
    const frontendRoot = new URL('../', document.currentScript.src);
    window.EventCampusImages = {
        resolve(path) {
            const value = String(path);
            const url = value.startsWith('/frontend/')
                ? new URL(value.slice('/frontend/'.length), frontendRoot)
                : new URL(value, document.baseURI);
            if (!['http:', 'https:'].includes(url.protocol)) throw new Error('Unsupported image URL');
            return url;
        }
    };
})();
