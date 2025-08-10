// Webpack configuration for Compose Multiplatform JS/WASM targets with SQL.js support
const path = require('path');

// Add fallbacks for Node.js modules that sql.js tries to use in browser
config.resolve = config.resolve || {};
config.resolve.fallback = {
    ...config.resolve.fallback,
    "fs": false,
    "path": false,
    "crypto": false
};

if (config.devServer && process.env.KMP_TARGET === 'wasmJs') {
    // Enable WebAssembly support for WASM targets
    config.experiments = config.experiments || {};
    config.experiments.asyncWebAssembly = true;
    // Add proper headers for WASM/SharedArrayBuffer support when needed
    config.devServer.headers = {
        ...config.devServer.headers,
        'Cross-Origin-Embedder-Policy': 'require-corp',
        'Cross-Origin-Opener-Policy': 'same-origin',
    };
} else {
    // Force sql.js to use the JS-only version instead of WASM
    config.resolve.alias = {
        ...config.resolve.alias,
        'sql.js': 'sql.js/dist/sql-js.js'
    };
}