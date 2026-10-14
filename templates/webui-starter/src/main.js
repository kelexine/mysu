import {
  exec,
  toast,
  fullScreen,
  enableEdgeToEdge,
  moduleInfo,
  listPackages,
  getPackagesInfo,
  exit,
  isMySuWebui,
} from '@mysu-org/mysu';

// Initialize Edge-to-Edge safe drawing
enableEdgeToEdge(true);

let isImmersive = false;

// DOM Elements
const modNameEl = document.getElementById('mod-name');
const modVersionEl = document.getElementById('mod-version');
const infoIdEl = document.getElementById('info-id');
const infoAuthorEl = document.getElementById('info-author');
const infoDirEl = document.getElementById('info-dir');
const infoEnvEl = document.getElementById('info-env');
const inputCmdEl = document.getElementById('input-cmd');
const btnRunEl = document.getElementById('btn-run');
const outputTerminalEl = document.getElementById('output-terminal');
const btnLoadAppsEl = document.getElementById('btn-load-apps');
const appsContainerEl = document.getElementById('apps-container');
const btnToastEl = document.getElementById('btn-toast');
const btnRebootEl = document.getElementById('btn-reboot');
const btnFullscreenEl = document.getElementById('btn-fullscreen');
const btnExitEl = document.getElementById('btn-exit');

// 1. Initialize Module Information & Environment Check
function initModuleMetadata() {
  const isOnline = isMySuWebui();

  if (isOnline) {
    infoEnvEl.textContent = 'MySU WebView';
    infoEnvEl.classList.add('online');

    try {
      const rawInfo = moduleInfo();
      if (rawInfo) {
        const info = JSON.parse(rawInfo);
        modNameEl.textContent = info.name || 'MySU Module';
        modVersionEl.textContent = `v${info.version || '1.0.0'}`;
        infoIdEl.textContent = info.id || '-';
        infoAuthorEl.textContent = info.author || '-';
        infoDirEl.textContent = info.moduleDir || '-';
      }
    } catch (err) {
      console.error('Failed to parse moduleInfo:', err);
    }
  } else {
    infoEnvEl.textContent = 'Browser (Standalone)';
    infoEnvEl.classList.add('offline');
    modNameEl.textContent = 'WebUI Starter Demo';
    modVersionEl.textContent = 'v1.0.0-mock';
    infoIdEl.textContent = 'mysu_webui_starter';
    infoAuthorEl.textContent = 'kelexine';
    infoDirEl.textContent = '/data/adb/modules/mysu_webui_starter';
  }
}

// 2. Command Runner
async function runCommand() {
  const cmd = inputCmdEl.value.trim();
  if (!cmd) return;

  btnRunEl.disabled = true;
  outputTerminalEl.textContent = `Executing: ${cmd}...\n`;

  try {
    if (isMySuWebui()) {
      const { errno, stdout, stderr } = await exec(cmd);
      if (errno === 0) {
        outputTerminalEl.textContent = stdout || '(No output produced)';
      } else {
        outputTerminalEl.textContent = `[Exit ${errno}]\n${stderr || stdout || 'Command failed'}`;
      }
    } else {
      // Mock output when testing in desktop browser
      await new Promise((r) => setTimeout(r, 600));
      outputTerminalEl.textContent = `[Mock Execution]\nDevice: CachyOS / Android\nOutput: ${cmd} ran successfully.`;
    }
  } catch (err) {
    outputTerminalEl.textContent = `Error: ${err.message}`;
  } finally {
    btnRunEl.disabled = false;
  }
}

// 3. Load Applications & Display via mysu://icon/
async function loadApps() {
  btnLoadAppsEl.disabled = true;
  appsContainerEl.innerHTML = '<p class="placeholder-text">Querying installed packages...</p>';

  try {
    let packages = [];

    if (isMySuWebui()) {
      const packageNames = await listPackages('user');
      // Limit to first 25 apps for snappy demo rendering
      const subset = packageNames.slice(0, 25);
      packages = await getPackagesInfo(subset);
    } else {
      // Browser Mock
      packages = [
        { packageName: 'com.android.settings', appLabel: 'Settings', versionName: '15' },
        { packageName: 'com.android.chrome', appLabel: 'Chrome', versionName: '133.0' },
        { packageName: 'dev.kelexine.mysu', appLabel: 'MySU', versionName: '1.0.14' },
      ];
    }

    if (packages.length === 0) {
      appsContainerEl.innerHTML = '<p class="placeholder-text">No user applications found.</p>';
      return;
    }

    appsContainerEl.innerHTML = '';
    packages.forEach((app) => {
      const item = document.createElement('div');
      item.className = 'app-item';

      // Custom icon URI scheme: mysu://icon/{packageName}
      const iconUrl = isMySuWebui()
        ? `mysu://icon/${app.packageName}`
        : 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="%238b5cf6"><circle cx="12" cy="12" r="10"/></svg>';

      item.innerHTML = `
        <img class="app-icon" src="${iconUrl}" alt="${app.appLabel}" loading="lazy" />
        <div class="app-details">
          <span class="app-label">${app.appLabel || app.packageName}</span>
          <span class="app-pkg">${app.packageName} (v${app.versionName || '1.0'})</span>
        </div>
      `;
      appsContainerEl.appendChild(item);
    });
  } catch (err) {
    appsContainerEl.innerHTML = `<p class="placeholder-text" style="color: var(--danger-color)">Error loading apps: ${err.message}</p>`;
  } finally {
    btnLoadAppsEl.disabled = false;
  }
}

// Event Listeners
btnRunEl.addEventListener('click', runCommand);
inputCmdEl.addEventListener('keydown', (e) => {
  if (e.key === 'Enter') runCommand();
});

btnLoadAppsEl.addEventListener('click', loadApps);

btnToastEl.addEventListener('click', () => {
  if (isMySuWebui()) {
    toast('Hello from MySU WebUI Starter!');
  } else {
    alert('Toast: Hello from MySU WebUI Starter!');
  }
});

btnRebootEl.addEventListener('click', async () => {
  const confirmed = confirm('Perform a quick userspace soft-reboot?');
  if (!confirmed) return;

  if (isMySuWebui()) {
    toast('Initiating soft reboot...');
    await exec('setprop ctl.restart zygote || reboot userspace');
  } else {
    alert('Mock reboot triggered.');
  }
});

btnFullscreenEl.addEventListener('click', () => {
  isImmersive = !isImmersive;
  fullScreen(isImmersive);
  btnFullscreenEl.textContent = isImmersive ? '🗗' : '⛶';
});

btnExitEl.addEventListener('click', () => {
  if (isMySuWebui()) {
    exit();
  } else {
    window.close();
  }
});

// Initial startup
initModuleMetadata();
