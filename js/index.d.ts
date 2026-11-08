/**
 * @mysu-org/mysu
 * Type definitions for MySU Module WebUI client SDK.
 *
 * Author: kelexine <https://github.com/kelexine>
 * License: Apache-2.0
 */

export interface ExecOptions {
  cwd?: string;
  env?: { [key: string]: string };
}

export interface ExecResults {
  errno: number;
  stdout: string;
  stderr: string;
}

export interface SpawnOptions {
  cwd?: string;
  env?: { [key: string]: string };
}

export interface Stdio {
  on(event: 'data', callback: (data: string) => void): void;
  emit(event: string, ...args: any[]): void;
}

export interface ChildProcess {
  stdin: Stdio;
  stdout: Stdio;
  stderr: Stdio;
  on(event: 'exit', callback: (code: number) => void): void;
  on(event: 'error', callback: (err: Error) => void): void;
  emit(event: string, ...args: any[]): void;
}

export interface ModuleInfo {
  id: string;
  name?: string;
  version?: string;
  versionCode?: number;
  author?: string;
  description?: string;
  moduleDir?: string;
  [key: string]: any;
}

export interface PackagesInfo {
  packageName: string;
  versionName: string;
  versionCode: number;
  appLabel: string;
  isSystem: boolean;
  uid: number;
  error?: string;
}

/**
 * Execute a command in an isolated root shell asynchronously.
 */
export function exec(command: string): Promise<ExecResults>;
export function exec(command: string, options: ExecOptions): Promise<ExecResults>;

/**
 * Spawn a continuous streaming process in a root shell.
 */
export function spawn(command: string): ChildProcess;
export function spawn(command: string, args: string[]): ChildProcess;
export function spawn(command: string, options: SpawnOptions): ChildProcess;
export function spawn(command: string, args: string[], options: SpawnOptions): ChildProcess;

/**
 * Toggle fullscreen mode, hiding or showing system status and navigation bars.
 */
export function fullScreen(isFullScreen: boolean): void;

/**
 * Enable or disable dynamic edge-to-edge window insets.
 */
export function enableEdgeToEdge(enable?: boolean): void;

/**
 * Show a native Android Toast notification.
 */
export function toast(message: string): void;

/**
 * Retrieve raw JSON string containing information about the current module.
 */
export function moduleInfo(): string;

/**
 * Retrieve parsed information object about the current module.
 */
export function getModuleInfo(): ModuleInfo;

/**
 * Query an Android system property asynchronously.
 */
export function getProp(propName: string, defaultValue?: string): Promise<string>;

/**
 * Return virtual icon resource URI for an installed package (`mysu://icon/{packageName}`).
 */
export function getAppIconUrl(packageName: string): string;

/**
 * List installed package names filtered by application type.
 */
export function listPackages(type?: 'all' | 'user' | 'system'): Promise<string[]>;

/**
 * Retrieve metadata for a list of package names.
 */
export function getPackagesInfo(packages: string[] | string): Promise<PackagesInfo[]>;

/**
 * Request the WebUI Activity to finish and return to the MySU Manager.
 */
export function exit(): void;

/**
 * Check whether the current web runtime is running inside the MySU WebUI environment.
 */
export function isMySuWebui(): boolean;
