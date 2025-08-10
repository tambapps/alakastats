// Webpack configuration for Compose Multiplatform JS/WASM targets with SQL.js support
const path = require('path');

// Add fallbacks for Node.js modules that sql.js tries to use in browser
config.resolve = config.resolve || {};
config.resolve.fallback = {
    ...config.resolve.fallback,
    "fs": false,
    "path": false,
    "crypto": false,
    "util": false,
    "assert": false,
    "buffer": false,
    "stream": false
};

// Ignore Node.js module resolution errors to make them non-blocking
config.ignoreWarnings = [
    /Module not found: Error: Can't resolve 'fs'/,
    /Module not found: Error: Can't resolve 'path'/, 
    /Module not found: Error: Can't resolve 'crypto'/,
    /BREAKING CHANGE: webpack < 5 used to include polyfills/
];

// Fix Skiko function mangling in production builds
if (config.mode === 'production') {
    config.optimization = config.optimization || {};
    
    // Preserve Skiko function names by excluding skiko.js from minification
    config.optimization.minimize = false; // Disable minification entirely for now
    
    // Alternative: preserve skiko functions specifically
    // config.module = config.module || {};
    // config.module.rules = config.module.rules || [];
    // config.module.rules.push({
    //     test: /skiko\.js$/,
    //     use: 'raw-loader'
    // });
}

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