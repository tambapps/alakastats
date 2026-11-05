// The app's resources, read by the tests opening the real app: the test server doesn't serve them by itself
const composeResources = require("path").resolve(
    config.basePath,
    "../../../../composeApp/build/kotlin-multiplatform-resources/aggregated-resources/wasmJs/composeResources"
);
config.files.push({ pattern: `${composeResources}/**`, included: false, served: true, watched: false });
config.proxies = { ...config.proxies, "/composeResources/": `/absolute${composeResources}/` };
