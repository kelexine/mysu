# @mysu-org/mysu

Official JavaScript & TypeScript client library for MySU Module WebUI.

Provides asynchronous bindings to the native `window.mysu` bridge injected into module WebViews by the MySU Android Manager.

## Installation

```sh
# Using bun (recommended)
bun add @mysu-org/mysu

# Using pnpm
pnpm add @mysu-org/mysu

# Using npm
npm install @mysu-org/mysu

# Using yarn
yarn add @mysu-org/mysu
```

## Architecture Note

The Manager renders module WebUIs via an internal AndroidX `WebViewAssetLoader` mapped to `https://mui.mysu.org/` pointing directly to `/data/adb/modules/<id>/webroot/` (or `/data/adb/mysu/modules/<id>/webroot/`).

> [!NOTE]
> `mui.mysu.org` is strictly an internal on-device virtual origin intercepted locally by `WebViewAssetLoader`. It does **not** resolve over the internet, and no public server exists. All assets are loaded offline from the module's `webroot/` directory.

---

## API Reference

### `exec(command, options?)`

Executes a command inside an isolated root shell, returning a Promise that resolves with exit code, `stdout`, and `stderr`.

- **`command`** `<string>`: Shell command line to run.
- **`options`** `<ExecOptions>` *(optional)*:
  - `cwd` `<string>`: Working directory for the process.
  - `env` `<Record<string, string>>`: Custom environment variables.
- **Returns**: `Promise<{ errno: number, stdout: string, stderr: string }>`

```javascript
import { exec } from '@mysu-org/mysu';

const { errno, stdout, stderr } = await exec('getprop ro.product.model', {
  cwd: '/data/local/tmp'
});

if (errno === 0) {
  console.log('Model:', stdout.trim());
} else {
  console.error('Command failed:', stderr);
}
```

---

### `spawn(command, args?, options?)`

Spawns a continuous streaming process in a root shell. Returns a `ChildProcess` instance emitting real-time `stdout`, `stderr`, `exit`, and `error` events.

- **`command`** `<string>`: Binary or command name.
- **`args`** `<string[]>` *(optional)*: Command-line arguments.
- **`options`** `<SpawnOptions>` *(optional)*:
  - `cwd` `<string>`: Working directory.
  - `env` `<Record<string, string>>`: Custom environment variables.
- **Returns**: `ChildProcess`

```javascript
import { spawn } from '@mysu-org/mysu';

const monitor = spawn('logcat', ['-v', 'time', '-s', 'MySU:V']);

monitor.stdout.on('data', (chunk) => {
  console.log('stdout:', chunk);
});

monitor.stderr.on('data', (chunk) => {
  console.error('stderr:', chunk);
});

monitor.on('exit', (code) => {
  console.log(`Process exited with code ${code}`);
});

monitor.on('error', (err) => {
  console.error('Spawn error:', err);
});
```

---

### `toast(message)`

Displays a native Android toast notification.

- **`message`** `<string>`: Text to display.

```javascript
import { toast } from '@mysu-org/mysu';

toast('Settings applied successfully');
```

---

### `moduleInfo()`

Returns metadata about the currently active module parsed from `module.prop`.

- **Returns**: `string` (JSON-encoded module properties, e.g. `id`, `name`, `version`, `versionCode`, `author`, `description`, `moduleDir`).

```javascript
import { moduleInfo } from '@mysu-org/mysu';

const info = JSON.parse(moduleInfo());
console.log(`Loaded ${info.name} v${info.version} (${info.id})`);
```

---

### `listPackages(type)`

Queries installed packages on the device filtered by application type.

- **`type`** `<string>`: `'user'`, `'system'`, or `'all'`.
- **Returns**: `Promise<string[]>`

```javascript
import { listPackages } from '@mysu-org/mysu';

const userApps = await listPackages('user');
console.log(`Found ${userApps.length} installed user apps`);
```

---

### `getPackagesInfo(packages)`

Fetches detailed package metadata (labels, version name/code, UID, system flag) for the specified package names in batch.

- **`packages`** `<string[] | string>`: Array of package names or JSON array string.
- **Returns**: `Promise<PackagesInfo[]>`

```javascript
import { getPackagesInfo } from '@mysu-org/mysu';

const infoList = await getPackagesInfo(['com.android.settings', 'com.google.android.gms']);

infoList.forEach((pkg) => {
  console.log(`${pkg.appLabel} (${pkg.packageName}) - UID ${pkg.uid}, v${pkg.versionName}`);
});
```

#### `PackagesInfo` Structure

```typescript
interface PackagesInfo {
  packageName: string;
  versionName: string;
  versionCode: number;
  appLabel: string;
  isSystem: boolean;
  uid: number;
  error?: string;
}
```

---

### App Icon Protocol: `mysu://icon/{packageName}`

When querying packages via `listPackages` or `getPackagesInfo`, you can display high-resolution application icons directly in standard HTML image tags:

```html
<img src="mysu://icon/com.android.settings" alt="Settings Icon" width="48" height="48" />
```

The Manager intercepts the `mysu://icon/` scheme and serves cached 512×512 PNG icons with proper CORS headers (`Access-Control-Allow-Origin: *`).

---

### `fullScreen(isFullScreen)`

Toggles full-screen immersive mode for the module WebView.

- **`isFullScreen`** `<boolean>`: `true` to hide system bars, `false` to restore them.

```javascript
import { fullScreen } from '@mysu-org/mysu';

fullScreen(true);
```

---

### `enableEdgeToEdge(enable)`

Configures window insets and safe-area padding for edge-to-edge layouts.

- **`enable`** `<boolean>`: `true` to enable edge-to-edge drawing.

To automatically synchronize dynamic safe-area insets (status bar, navigation bar, cutouts), import `/internal/insets.css` in your HTML or CSS:

```html
<!-- In index.html -->
<link rel="stylesheet" type="text/css" href="/internal/insets.css" />
```

Or in your CSS:

```css
@import "https://mui.mysu.org/internal/insets.css";

body {
  padding-top: var(--inset-top, 0px);
  padding-bottom: var(--inset-bottom, 0px);
  padding-left: var(--inset-left, 0px);
  padding-right: var(--inset-right, 0px);
}
```

---

### `isMySuWebui()`

Detects whether the JavaScript context is executing inside the MySU Manager WebView with the `window.mysu` bridge present.

- **Returns**: `boolean`

```javascript
import { isMySuWebui } from '@mysu-org/mysu';

if (!isMySuWebui()) {
  console.warn('Running outside MySU WebView — mock environment activated');
}
```

---

### `exit()`

Closes the module WebUI activity and returns the user to the MySU Manager module list.

```javascript
import { exit } from '@mysu-org/mysu';

exit();
```

---

## License

Apache-2.0 © kelexine
