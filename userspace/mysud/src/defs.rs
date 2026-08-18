#[cfg(target_os = "android")]
mod android {
    use const_format::concatcp;

    pub const ADB_DIR: &str = "/data/adb/";
    pub const WORKING_DIR: &str = concatcp!(ADB_DIR, "mysu/");
    pub const BINARY_DIR: &str = concatcp!(WORKING_DIR, "bin/");
    pub const LIBRARY_DIR: &str = concatcp!(WORKING_DIR, "lib/");
    pub const LOG_DIR: &str = concatcp!(WORKING_DIR, "log/");
    pub const SULOGD_LOCK_PATH: &str = concatcp!(WORKING_DIR, "sulogd.lock");

    pub const PROFILE_DIR: &str = concatcp!(WORKING_DIR, "profile/");
    pub const PROFILE_SELINUX_DIR: &str = concatcp!(PROFILE_DIR, "selinux/");
    pub const PROFILE_TEMPLATE_DIR: &str = concatcp!(PROFILE_DIR, "templates/");

    pub const MYSURC_PATH: &str = concatcp!(WORKING_DIR, ".mysurc");
    pub const DAEMON_PATH: &str = concatcp!(ADB_DIR, "mysud");
    pub const LIBADBROOT_PATH: &str = concatcp!(LIBRARY_DIR, "libadbroot.so");

    pub const DAEMON_LINK_PATH: &str = concatcp!(BINARY_DIR, "mysud");

    pub const MODULE_DIR: &str = concatcp!(WORKING_DIR, "modules/");
    pub const MODULE_UPDATE_DIR: &str = concatcp!(WORKING_DIR, "modules_update/");
    pub const METAMODULE_DIR: &str = concatcp!(WORKING_DIR, "metamodule/");

    // Prefer /metadata/watchdog/ when present, else /metadata
    pub const PREINIT_DIR_WATCHDOG: &str = "/metadata/watchdog/mysu/";
    pub const PREINIT_DIR_DEFAULT: &str = "/metadata/mysu/";
    pub const MODULES_RC_FILE: &str = "modules.rc";
    pub const MODULES_RC_TMP_FILE: &str = ".modules.rc.tmp";

    pub const MODULE_WEB_DIR: &str = "webroot";
    pub const MODULE_ACTION_SH: &str = "action.sh";
    pub const DISABLE_FILE_NAME: &str = "disable";
    pub const UPDATE_FILE_NAME: &str = "update";
    pub const REMOVE_FILE_NAME: &str = "remove";
    pub const MODULE_INIT_RC_DIR: &str = "initrc";

    // Module config system
    pub const MODULE_CONFIG_DIR: &str = concatcp!(WORKING_DIR, "module_configs/");
    pub const PERSIST_CONFIG_NAME: &str = "persist.config";
    pub const TEMP_CONFIG_NAME: &str = "tmp.config";

    // Metamodule support
    pub const METAMODULE_MOUNT_SCRIPT: &str = "metamount.sh";
    pub const METAMODULE_METAINSTALL_SCRIPT: &str = "metainstall.sh";
    pub const METAMODULE_METAUNINSTALL_SCRIPT: &str = "metauninstall.sh";

    pub const MYSU_BACKUP_DIR: &str = WORKING_DIR;
    pub const MYSU_BACKUP_FILE_PREFIX: &str = "mysu_backup_";
    pub const BACKUP_FILENAME: &str = "stock_image.sha1";
    pub const MYSU_TEMP_BACKUP_DIR_NAME: &str = "boot_backup";

    pub const DEFAULT_PACKAGE_NAME: &str = env!("MYSU_PACKAGE_NAME");
}

#[allow(unused)]
pub const VERSION_CODE: &str = env!("VERSION_CODE");
pub const VERSION_NAME: &str = env!("VERSION_NAME");
#[cfg(target_os = "android")]
pub const FULL_VERSION: &str = const_format::formatcp!(
    "{VERSION_NAME} (uapi: {})",
    crate::mysu_uapi::KERNEL_SU_UAPI_VERSION
);

#[cfg(target_os = "android")]
pub use android::*;
