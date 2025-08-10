// Webpack configuration for Compose Multiplatform JS target
const path = require('path');

// Ensure WASM files are properly loaded
if (config.devServer) {
    config.devServer.static = config.devServer.static || [];
    config.devServer.static.push({
        directory: path.resolve(__dirname, '../composeApp/build/processedResources/js/main'),
        publicPath: '/',
    });
}

// Handle WASM loading
config.experiments = config.experiments || {};
config.experiments.asyncWebAssembly = true;

// Add proper headers for WASM
if (config.devServer) {
    config.devServer.headers = {
        'Cross-Origin-Embedder-Policy': 'require-corp',
        'Cross-Origin-Opener-Policy': 'same-origin',
    };
}