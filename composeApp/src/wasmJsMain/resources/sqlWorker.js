// SQL Worker creation for WASM target (ES module)
function createSqlWorker() {
    try {
        // Use import.meta.url for ES modules in WASM
        return new Worker(new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url));
    } catch (e) {
        console.error('Failed to create SQL.js worker:', e);
        throw e;
    }
}

// Export for WASM usage
globalThis.createSqlWorker = createSqlWorker;