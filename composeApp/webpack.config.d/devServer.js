// The repository and composeApp are served to debug the Kotlin sources in the browser, but not watched: watching them
// (.git, .gradle, build outputs...) opens more files than allowed and the dev server crashes (EMFILE: too many open
// files, watch). The app still reloads, webpack watches what it bundles
if (config.devServer && Array.isArray(config.devServer.static)) {
    config.devServer.static = config.devServer.static.map(dir =>
        typeof dir === "string" ? { directory: dir, watch: false } : { ...dir, watch: false }
    );
}
// The URLs of the screens (e.g. /pokedex/metagross) have no file: the app's page is served for them, as 404.html does
// once deployed
if (config.devServer) {
    config.devServer.historyApiFallback = true;
}
