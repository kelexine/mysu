# Module WebUI

In addition to executing boot scripts and mounting systemless modifications, MySU modules can provide rich web-based graphical user interfaces that render directly inside the MySU Android Manager.

Modules define standard HTML, CSS, and JavaScript applications packaged into a `webroot` directory. The Manager renders them in an isolated, hardware-accelerated WebView and injects a secure root-level bridge (`window.mysu`) accessible via the official client library [`@mysu-org/mysu`](https://www.npmjs.com/package/@mysu-org/mysu).

---

## 1. Architecture & Sandbox

### The `webroot` Directory

All web assets must reside in the `webroot` subdirectory of your module directory, with `index.html` serving as the main entry point:

```txt
my-module/
├── module.prop
├── post-fs-data.sh
├── service.sh
└── webroot/
    ├── index.html
    ├── assets/
    │   ├── index.css
    │   └── index.js
    └── favicon.ico
```

::: warning Permissions and SELinux
When installing or updating a module, MySU automatically sets appropriate directory permissions and SELinux contexts for `webroot/`. Do not manually alter filesystem permissions or contexts for `webroot/` in installation scripts unless strictly necessary.
:::

### Internal Virtual Authority

The Manager intercepts WebView requests using AndroidX `WebViewAssetLoader` configured for the domain `mui.mysu.org`:

- **Path Mapping**: `https://mui.mysu.org/*` is mapped locally and offline to `/data/adb/modules/<id>/webroot/*` (or `/data/adb/mysu/modules/<id>/webroot/*`).
- **No Public DNS**: `mui.mysu.org` is strictly an **internal on-device virtual authority**. There is no public `*.mysu.org` or `mysu.org` website on the internet. All asset requests are resolved completely offline from the local module directory on the device.
- **Relative Pathing**: Always build your single-page applications with **relative base URLs** (`./` instead of `/`) so script, style, and media assets resolve reliably.

---

## 2. Client SDK (`@mysu-org/mysu`)

The official client package provides TypeScript typings and clean async/await promises around the injected `window.mysu` bridge.

### Installation

```bash
# Using bun (recommended)
bun add @mysu-org/mysu

# Using pnpm
pnpm add @mysu-org/mysu

# Using npm
npm install @mysu-org/mysu
```

### Checking Environment

Before invoking APIs, you can check whether your page is running inside the MySU Manager:

```javascript
import { isMySuWebui } from '@mysu-org/mysu';

if (isMySuWebui()) {
  console.log('Running inside MySU WebView');
} else {
  console.warn('Running outside MySU (development/browser fallback)');
}
```

---

## 3. Core Bridge APIs

### Executing Commands (`exec`)

Runs a shell command inside an isolated root shell and resolves when execution completes:

```javascript
import { exec } from '@mysu-org/mysu';

// Basic command
const { errno, stdout, stderr } = await exec('getprop ro.product.model');
if (errno === 0) {
  console.log('Device Model:', stdout.trim());
}

// Running with custom working directory and environment variables
const result = await exec('./my_daemon --status', {
  cwd: '/data/adb/modules/my_module',
  env: {
    MY_CONFIG: '1',
    PATH: '/data/adb/mysu/bin:/system/bin:/vendor/bin'
  }
});
```

### Real-Time Streaming Processes (`spawn`)

For long-running tasks, live monitors, or compilation output, use `spawn` to stream `stdout` and `stderr` in real time:

```javascript
import { spawn } from '@mysu-org/mysu';

const monitor = spawn('logcat', ['-v', 'time', '-s', 'MySU:V']);

monitor.stdout.on('data', (chunk) => {
  console.log('Log chunk:', chunk);
});

monitor.stderr.on('data', (errChunk) => {
  console.error('Log error:', errChunk);
});

monitor.on('exit', (code) => {
  console.log(`Logcat exited with code: ${code}`);
});

monitor.on('error', (err) => {
  console.error('Spawn error:', err);
});
```

### Toast Notifications

```javascript
import { toast } from '@mysu-org/mysu';

toast('Configuration saved successfully');
```

### Module Metadata (`moduleInfo`)

Retrieve metadata about the currently executing module from `module.prop`:

```javascript
import { moduleInfo } from '@mysu-org/mysu';

const info = JSON.parse(moduleInfo());
console.log(`Module: ${info.name} (${info.id}) v${info.version}`);
console.log(`Module directory: ${info.moduleDir}`);
```

### Exiting WebUI

Close the WebView activity and return to the Manager:

```javascript
import { exit } from '@mysu-org/mysu';

exit();
```

---

## 4. Package Queries & App Icon Protocol

MySU WebUI includes dedicated APIs for querying installed applications and displaying high-resolution icons.

### Listing Packages

```javascript
import { listPackages } from '@mysu-org/mysu';

// Type options: 'user', 'system', or 'all'
const userPackages = await listPackages('user');
console.log('Installed user packages:', userPackages);
```

### Querying Detailed Package Info

Batch-query app labels, versions, UIDs, and system flags:

```javascript
import { getPackagesInfo } from '@mysu-org/mysu';

const packages = await getPackagesInfo(['com.android.settings', 'com.android.chrome']);

packages.forEach((pkg) => {
  console.log(`${pkg.appLabel} (${pkg.packageName}) - UID ${pkg.uid}, v${pkg.versionName}`);
});
```

### Rendering App Icons (`mysu://icon/`)

The Manager provides a custom URI protocol to stream cached PNG application icons directly into HTML image tags:

```html
<img src="mysu://icon/com.android.settings" alt="Settings Icon" width="48" height="48" />
```

In Vue, React, or standard JS:

```javascript
function getAppIconUri(packageName) {
  return `mysu://icon/${packageName}`;
}
```

---

## 5. UI Layout, Safe Areas & Fullscreen

### Dynamic Safe Area Insets (`internal/insets.css`)

To properly handle status bars, display cutouts, and gesture navigation bars on modern edge-to-edge Android displays, MySU exposes dynamic safe insets:

1. Add the insets stylesheet link in your `index.html`:
   ```html
   <link rel="stylesheet" type="text/css" href="/internal/insets.css" />
   ```
2. Enable edge-to-edge drawing in your application startup:
   ```javascript
   import { enableEdgeToEdge } from '@mysu-org/mysu';

   enableEdgeToEdge(true);
   ```
3. Use the CSS custom properties in your root layout:
   ```css
   body {
     padding-top: var(--inset-top, 0px);
     padding-bottom: var(--inset-bottom, 0px);
     padding-left: var(--inset-left, 0px);
     padding-right: var(--inset-right, 0px);
   }
   ```

### Fullscreen Immersive Mode

```javascript
import { fullScreen } from '@mysu-org/mysu';

// Enter immersive fullscreen
fullScreen(true);

// Restore navigation and status bars
fullScreen(false);
```

---

## 6. Idiomatic Filesystem & Shell Helpers

Derived from production modules like [`encore`](https://github.com/kelexine/encore), here are recommended helper functions using `exec`:

```javascript
import { exec, toast } from '@mysu-org/mysu';

/**
 * Read text file from root filesystem
 */
export async function readFile(path) {
  const { errno, stdout, stderr } = await exec(`[ -f "${path}" ] && cat "${path}"`);
  if (errno !== 0) throw new Error(stderr || 'File not found');
  return stdout.trim();
}

/**
 * Safely write string to file
 */
export async function writeFile(path, content) {
  const escaped = content.replace(/'/g, "'\\''");
  const { errno, stderr } = await exec(`echo '${escaped}' > "${path}"`);
  if (errno !== 0) throw new Error(stderr || 'Failed to write file');
}

/**
 * Check file existence
 */
export async function fileExists(path) {
  const { errno } = await exec(`[ -f "${path}" ]`);
  return errno === 0;
}

/**
 * Launch an installed application
 */
export async function launchApp(packageName) {
  await exec(`monkey -p ${packageName} -c android.intent.category.LAUNCHER 1`);
}

/**
 * Open external URL in user's browser
 */
export async function openUrl(url) {
  await exec(`am start -a android.intent.action.VIEW -d "${url}"`);
}
```

---

## 7. Build & Deployment Workflow

### Packaging with Vite & Bun

For building modern single-page applications, **Vite** with **Bun** is the recommended toolchain:

1. In your `vite.config.js`, configure a relative base and output directory:
   ```javascript
   import { defineConfig } from 'vite';

   export default defineConfig({
     base: './', // CRITICAL: Ensures relative paths for offline WebView
     build: {
       outDir: '../module/webroot', // Output directly to module folder
       emptyOutDir: true
     }
   });
   ```
2. Build the project:
   ```bash
   bun run build
   ```
3. Test locally or flash your module ZIP via MySU Manager. When opening the module in Manager, click the **WebUI** action button on the module card to launch your interface.

---

## 8. WebUI Starter Template

A zero-config starter template is available in the repository at [`templates/webui-starter/`](https://github.com/kelexine/mysu/tree/main/templates/webui-starter):

```bash
cd templates/webui-starter
bun install
bun run build
```

This template includes working implementations of `@mysu-org/mysu`, safe area insets, terminal commands, and app package browsing with icon loading.
