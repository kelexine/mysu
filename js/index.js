/**
 * @mysu-org/mysu
 * Official JavaScript & TypeScript client library for MySU Module WebUI.
 *
 * Author: kelexine <https://github.com/kelexine>
 * License: Apache-2.0
 */

let callbackCounter = 0;
function getUniqueCallbackName(prefix) {
  return `${prefix}_callback_${Date.now()}_${callbackCounter++}`;
}

function getBridge() {
  if (typeof window !== "undefined" && typeof window.mysu !== "undefined") {
    return window.mysu;
  }
  return null;
}

/**
 * Execute a command in a root shell asynchronously.
 *
 * @param {string} command - Shell command to execute.
 * @param {object} [options] - Optional execution configuration { cwd, env }.
 * @returns {Promise<{ errno: number, stdout: string, stderr: string }>}
 */
export function exec(command, options) {
  if (typeof options === "undefined") {
    options = {};
  }

  return new Promise((resolve, reject) => {
    const bridge = getBridge();
    if (!bridge) {
      return reject(new Error("MySU WebUI bridge (window.mysu) is not available."));
    }

    const callbackFuncName = getUniqueCallbackName("exec");

    window[callbackFuncName] = (errno, stdout, stderr) => {
      resolve({ errno, stdout, stderr });
      cleanup(callbackFuncName);
    };

    function cleanup(name) {
      delete window[name];
    }

    try {
      bridge.exec(command, JSON.stringify(options), callbackFuncName);
    } catch (error) {
      cleanup(callbackFuncName);
      reject(error);
    }
  });
}

function Stdio() {
  this.listeners = {};
}

Stdio.prototype.on = function (event, listener) {
  if (!this.listeners[event]) {
    this.listeners[event] = [];
  }
  this.listeners[event].push(listener);
};

Stdio.prototype.emit = function (event, ...args) {
  if (this.listeners[event]) {
    this.listeners[event].forEach((listener) => listener(...args));
  }
};

function ChildProcess() {
  this.listeners = {};
  this.stdin = new Stdio();
  this.stdout = new Stdio();
  this.stderr = new Stdio();
}

ChildProcess.prototype.on = function (event, listener) {
  if (!this.listeners[event]) {
    this.listeners[event] = [];
  }
  this.listeners[event].push(listener);
};

ChildProcess.prototype.emit = function (event, ...args) {
  if (this.listeners[event]) {
    this.listeners[event].forEach((listener) => listener(...args));
  }
};

/**
 * Spawn a continuous root process emitting streaming stdout/stderr/exit events.
 *
 * @param {string} command - Command name or executable path.
 * @param {string[]|object} [args] - Command line arguments, or options if args omitted.
 * @param {object} [options] - Process options { cwd, env }.
 * @returns {ChildProcess}
 */
export function spawn(command, args, options) {
  if (typeof args === "undefined") {
    args = [];
  } else if (!(args instanceof Array)) {
    options = args;
    args = [];
  }

  if (typeof options === "undefined") {
    options = {};
  }

  const child = new ChildProcess();
  const bridge = getBridge();
  if (!bridge) {
    setTimeout(() => {
      child.emit("error", new Error("MySU WebUI bridge (window.mysu) is not available."));
    }, 0);
    return child;
  }

  const childCallbackName = getUniqueCallbackName("spawn");
  window[childCallbackName] = child;

  function cleanup(name) {
    delete window[name];
  }

  child.on("exit", () => {
    cleanup(childCallbackName);
  });

  try {
    bridge.spawn(
      command,
      JSON.stringify(args),
      JSON.stringify(options),
      childCallbackName
    );
  } catch (error) {
    child.emit("error", error);
    cleanup(childCallbackName);
  }
  return child;
}

/**
 * Toggle fullscreen mode, hiding or showing system status and navigation bars.
 *
 * @param {boolean} isFullScreen
 */
export function fullScreen(isFullScreen) {
  const bridge = getBridge();
  if (bridge) bridge.fullScreen(Boolean(isFullScreen));
}

/**
 * Enable or disable dynamic edge-to-edge window insets.
 *
 * @param {boolean} [enable=true]
 */
export function enableEdgeToEdge(enable = true) {
  const bridge = getBridge();
  if (bridge) bridge.enableEdgeToEdge(Boolean(enable));
}

/**
 * Show a native Android Toast notification.
 *
 * @param {string} message - Text to display.
 */
export function toast(message) {
  const bridge = getBridge();
  if (bridge) bridge.toast(String(message));
}

/**
 * Retrieve raw JSON string containing information about the current module.
 *
 * @returns {string}
 */
export function moduleInfo() {
  const bridge = getBridge();
  return bridge ? bridge.moduleInfo() : "{}";
}

/**
 * Retrieve parsed information object about the current module.
 *
 * @returns {{ id: string, name: string, version: string, versionCode: number, author: string, description: string, moduleDir: string, [key: string]: any }}
 */
export function getModuleInfo() {
  try {
    return JSON.parse(moduleInfo());
  } catch {
    return {};
  }
}

/**
 * Query an Android system property asynchronously.
 *
 * @param {string} propName - System property key (e.g. "ro.product.model").
 * @param {string} [defaultValue=""] - Fallback if property is unset.
 * @returns {Promise<string>}
 */
export async function getProp(propName, defaultValue = "") {
  const bridge = getBridge();
  if (bridge && typeof bridge.getProp === "function") {
    return bridge.getProp(propName, defaultValue);
  }
  try {
    const res = await exec(`getprop ${propName}`);
    const val = res.stdout ? res.stdout.trim() : "";
    return val.length > 0 ? val : defaultValue;
  } catch {
    return defaultValue;
  }
}

/**
 * Return virtual icon resource URI for an installed package.
 *
 * @param {string} packageName - Application package name (e.g. "com.android.settings").
 * @returns {string} Virtual URI matching "mysu://icon/{packageName}".
 */
export function getAppIconUrl(packageName) {
  return `mysu://icon/${packageName}`;
}

/**
 * List installed package names filtered by application type.
 *
 * @param {"all"|"user"|"system"} [type="all"]
 * @returns {Promise<string[]>}
 */
export async function listPackages(type = "all") {
  const bridge = getBridge();
  if (!bridge) return [];
  try {
    return JSON.parse(bridge.listPackages(type));
  } catch {
    return [];
  }
}

/**
 * Retrieve metadata for a list of package names.
 *
 * @param {string[]|string} packages - Array of package names or JSON array string.
 * @returns {Promise<Array<{ packageName: string, versionName: string, versionCode: number, appLabel: string, isSystem: boolean, uid: number }>>}
 */
export async function getPackagesInfo(packages) {
  const bridge = getBridge();
  if (!bridge) return [];
  try {
    if (typeof packages !== "string") {
      packages = JSON.stringify(packages);
    }
    return JSON.parse(bridge.getPackagesInfo(packages));
  } catch {
    return [];
  }
}

/**
 * Request the WebUI Activity to finish and return to the MySU Manager.
 */
export function exit() {
  const bridge = getBridge();
  if (bridge) bridge.exit();
}

/**
 * Check whether the current web runtime is running inside the MySU WebUI environment.
 *
 * @returns {boolean}
 */
export function isMySuWebui() {
  return getBridge() !== null;
}
