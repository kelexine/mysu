import { defineConfig } from 'vite';

export default defineConfig({
  // CRITICAL: Base must be './' so assets resolve correctly via local WebViewAssetLoader
  base: './',
  build: {
    outDir: 'dist',
    emptyOutDir: true,
    assetsDir: 'assets',
  },
  server: {
    port: 5173,
    host: true,
  },
});
