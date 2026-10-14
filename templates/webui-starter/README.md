# MySU WebUI Starter Template

Modern, responsive starter template for building **MySU Module WebUIs** using **Vite**, **Bun**, and [`@mysu-org/mysu`](https://www.npmjs.com/package/@mysu-org/mysu).

---

## Features

- **Official Client SDK**: Preconfigured with `@mysu-org/mysu` (`exec`, `toast`, `fullScreen`, `listPackages`, `getPackagesInfo`, `moduleInfo`, `exit`).
- **Dynamic Insets**: Automatically integrates `/internal/insets.css` for edge-to-edge Android displays (status/nav bars, cutouts).
- **Custom Icon Scheme**: Demonstrates the `mysu://icon/{packageName}` protocol for streaming 512×512 app icons.
- **Root Command Runner**: Interactive terminal executing root shell commands with stdout/stderr capture.
- **Graceful Fallbacks**: Safe mocks when opened in a standard desktop browser for rapid UI iteration.
- **Relative Pathing**: Preconfigured with `base: './'` for offline Android WebView rendering.

---

## Architecture Note

The MySU Manager serves module WebUIs offline via AndroidX `WebViewAssetLoader` mapped to `https://mui.mysu.org/` pointing directly to `/data/adb/modules/<id>/webroot/`.

> [!NOTE]
> `mui.mysu.org` is strictly an internal, on-device virtual authority intercepted locally by the Manager WebView. It does **not** resolve on the public internet, and there is no public server. All assets and scripts are loaded offline directly from your module's `webroot/` directory.

---

## Getting Started

### 1. Install Dependencies

```bash
# Using bun (recommended)
bun install

# Or using npm / pnpm
npm install
```

### 2. Local Development

Start the local Vite development server for fast UI preview in your browser:

```bash
bun run dev
```

### 3. Build for Production

Compile and bundle the production assets into `dist/`:

```bash
bun run build
```

---

## Deploying to a Module

Copy the contents of `dist/` directly into your module's `webroot/` directory:

```bash
# Example module directory structure:
my-module/
├── module.prop
├── service.sh
└── webroot/
    ├── index.html
    └── assets/
        ├── index-*.js
        └── index-*.css
```

Alternatively, configure `vite.config.js` to output directly into your module's folder:

```javascript
// vite.config.js
export default defineConfig({
  base: './',
  build: {
    outDir: '../my-module/webroot',
    emptyOutDir: true,
  },
});
```

---

## License

Apache-2.0 © kelexine
