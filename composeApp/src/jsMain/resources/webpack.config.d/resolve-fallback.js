// Merge with the generated webpack config
config.resolve = config.resolve || {};
config.resolve.fallback = Object.assign({}, config.resolve.fallback, {
  path: require("path-browserify"),
  fs: false
});
