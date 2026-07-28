// Babel is used only by Jest (babel-jest) to transform JSX/ESM in tests.
// Vite handles its own transforms via esbuild for dev/build, so this config
// does not affect the production bundle.
module.exports = {
  presets: [
    ['@babel/preset-env', { targets: { node: 'current' } }],
    ['@babel/preset-react', { runtime: 'automatic' }],
  ],
}
