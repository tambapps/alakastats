// Webpack configuration for Compose Multiplatform JS target with SQL.js support
const path = require('path');

// Add fallbacks for Node.js modules that sql.js tries to use in browser
config.resolve = config.resolve || {};
config.resolve.fallback = {
    ...config.resolve.fallback,
    "fs": false,
    "path": false,
    "crypto": false
};

// Force sql.js to use the JS-only version instead of WASM
config.resolve.alias = {
    ...config.resolve.alias,
    'sql.js': 'sql.js/dist/sql-js.js'
};