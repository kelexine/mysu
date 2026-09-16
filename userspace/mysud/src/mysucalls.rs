#![allow(clippy::unreadable_literal)]
use anyhow::bail;

use crate::mysu_uapi;
use std::fs;
use std::os::fd::RawFd;
use std::sync::OnceLock;

// Global driver fd cache
static DRIVER_FD: OnceLock<RawFd> = OnceLock::new();
static INFO_CACHE: OnceLock<mysu_uapi::mysu_get_info_cmd> = OnceLock::new();

fn scan_driver_fd() -> Option<RawFd> {
    let fd_dir = fs::read_dir("/proc/self/fd").ok()?;

    for entry in fd_dir.flatten() {
        if let Ok(fd_num) = entry.file_name().to_string_lossy().parse::<i32>() {
            let link_path = format!("/proc/self/fd/{fd_num}");
            if let Ok(target) = fs::read_link(&link_path) {
                let target_str = target.to_string_lossy();
                if target_str.contains("[mysu_driver]") || target_str.contains("[mysu_driver]") {
                    return Some(fd_num);
                }
            }
        }
    }

    None
}

// Get cached driver fd
fn init_driver_fd() -> Option<RawFd> {
    let fd = scan_driver_fd();
    if fd.is_none() {
        let mut fd = -1;
        unsafe {
            libc::syscall(
                libc::SYS_reboot,
                mysu_uapi::MYSU_INSTALL_MAGIC1,
                mysu_uapi::MYSU_INSTALL_MAGIC2,
                0,
                &mut fd,
            );
        };
        if fd >= 0 { Some(fd) } else { None }
    } else {
        fd
    }
}

// ioctl wrapper using libc
fn mysuctl<T>(request: u32, arg: *mut T) -> std::io::Result<i32> {
    use std::io;

    let fd = *DRIVER_FD.get_or_init(|| init_driver_fd().unwrap_or(-1));
    unsafe {
        let ret = libc::ioctl(fd as libc::c_int, request as i32, arg);
        if ret < 0 {
            Err(io::Error::last_os_error())
        } else {
            Ok(ret)
        }
    }
}

// API implementations
pub fn get_info() -> mysu_uapi::mysu_get_info_cmd {
    *INFO_CACHE.get_or_init(|| {
        let mut cmd = mysu_uapi::mysu_get_info_cmd {
            version: 0,
            flags: 0,
            features: 0,
            uapi_version: 0,
        };
        if mysuctl(mysu_uapi::MYSU_IOCTL_GET_INFO, &raw mut cmd).is_err() {
            let _ = mysuctl(mysu_uapi::MYSU_IOCTL_GET_INFO_LEGACY, &raw mut cmd);
        }
        cmd
    })
}

pub fn get_version() -> i32 {
    get_info().version as i32
}

pub fn is_late_load() -> bool {
    get_info().flags & mysu_uapi::MYSU_GET_INFO_FLAG_LATE_LOAD != 0
}

pub fn is_lkm() -> bool {
    get_info().flags & mysu_uapi::MYSU_GET_INFO_FLAG_LKM != 0
}

pub const fn uapi_version() -> u32 {
    mysu_uapi::MYSU_UAPI_VERSION
}

pub fn runtime_mode() -> &'static str {
    if is_late_load() {
        "late-load"
    } else if is_lkm() {
        "lkm"
    } else {
        "built-in"
    }
}

pub fn ensure_uapi_version_matched() -> anyhow::Result<()> {
    let kernel_uapi = get_info().uapi_version;
    let userspace_uapi = uapi_version();
    if kernel_uapi != userspace_uapi {
        bail!(
            "UAPI version mismatch: kernel={kernel_uapi}, mysud={userspace_uapi}. Please update MySU!"
        );
    }
    Ok(())
}

pub fn grant_root() -> std::io::Result<()> {
    mysuctl(mysu_uapi::MYSU_IOCTL_GRANT_ROOT, std::ptr::null_mut::<u8>())?;
    Ok(())
}

fn report_event(event: u32) {
    let mut cmd = mysu_uapi::mysu_report_event_cmd { event };
    let _ = mysuctl(mysu_uapi::MYSU_IOCTL_REPORT_EVENT, &raw mut cmd);
}

pub fn report_post_fs_data() {
    report_event(mysu_uapi::EVENT_POST_FS_DATA);
}

pub fn report_boot_complete() {
    report_event(mysu_uapi::EVENT_BOOT_COMPLETED);
}

pub fn report_module_mounted() {
    report_event(mysu_uapi::EVENT_MODULE_MOUNTED);
}

pub fn check_kernel_safemode() -> bool {
    let mut cmd = mysu_uapi::mysu_check_safemode_cmd { in_safe_mode: 0 };
    let _ = mysuctl(mysu_uapi::MYSU_IOCTL_CHECK_SAFEMODE, &raw mut cmd);
    cmd.in_safe_mode != 0
}

pub fn set_sepolicy(payload: *const u8, payload_len: u64) -> std::io::Result<i32> {
    let mut ioctl_cmd = crate::mysu_uapi::mysu_set_sepolicy_cmd {
        data_len: payload_len,
        data: payload as u64,
    };

    mysuctl(mysu_uapi::MYSU_IOCTL_SET_SEPOLICY, &raw mut ioctl_cmd)
}

/// Get feature value and support status from kernel
/// Returns (value, supported)
pub fn get_feature(feature_id: u32) -> std::io::Result<(u64, bool)> {
    let mut cmd = mysu_uapi::mysu_get_feature_cmd {
        feature_id,
        value: 0,
        supported: 0,
    };
    mysuctl(mysu_uapi::MYSU_IOCTL_GET_FEATURE, &raw mut cmd)?;
    Ok((cmd.value, cmd.supported != 0))
}

/// Set feature value in kernel
pub fn set_feature(feature_id: u32, value: u64) -> std::io::Result<()> {
    let mut cmd = mysu_uapi::mysu_set_feature_cmd { feature_id, value };
    mysuctl(mysu_uapi::MYSU_IOCTL_SET_FEATURE, &raw mut cmd)?;
    Ok(())
}

pub fn get_wrapped_fd(fd: RawFd) -> std::io::Result<RawFd> {
    let mut cmd = mysu_uapi::mysu_get_wrapper_fd_cmd {
        fd: fd as u32,
        flags: 0,
    };
    let result = mysuctl(mysu_uapi::MYSU_IOCTL_GET_WRAPPER_FD, &raw mut cmd)?;
    Ok(result)
}

pub fn get_sulog_fd() -> std::io::Result<RawFd> {
    let mut cmd = mysu_uapi::mysu_get_sulog_fd_cmd { flags: 0 };
    let result = mysuctl(mysu_uapi::MYSU_IOCTL_GET_SULOG_FD, &raw mut cmd)?;
    Ok(result)
}

/// Get mark status for a process (pid=0 returns total marked count)
pub fn mark_get(pid: i32) -> std::io::Result<u32> {
    let mut cmd = mysu_uapi::mysu_manage_mark_cmd {
        operation: mysu_uapi::MYSU_MARK_GET,
        pid,
        result: 0,
    };
    mysuctl(mysu_uapi::MYSU_IOCTL_MANAGE_MARK, &raw mut cmd)?;
    Ok(cmd.result)
}

/// Mark a process (pid=0 marks all processes)
pub fn mark_set(pid: i32) -> std::io::Result<()> {
    let mut cmd = mysu_uapi::mysu_manage_mark_cmd {
        operation: mysu_uapi::MYSU_MARK_MARK,
        pid,
        result: 0,
    };
    mysuctl(mysu_uapi::MYSU_IOCTL_MANAGE_MARK, &raw mut cmd)?;
    Ok(())
}

/// Unmark a process (pid=0 unmarks all processes)
pub fn mark_unset(pid: i32) -> std::io::Result<()> {
    let mut cmd = mysu_uapi::mysu_manage_mark_cmd {
        operation: mysu_uapi::MYSU_MARK_UNMARK,
        pid,
        result: 0,
    };
    mysuctl(mysu_uapi::MYSU_IOCTL_MANAGE_MARK, &raw mut cmd)?;
    Ok(())
}

/// Refresh mark for all running processes
pub fn mark_refresh() -> std::io::Result<()> {
    let mut cmd = mysu_uapi::mysu_manage_mark_cmd {
        operation: mysu_uapi::MYSU_MARK_REFRESH,
        pid: 0,
        result: 0,
    };
    mysuctl(mysu_uapi::MYSU_IOCTL_MANAGE_MARK, &raw mut cmd)?;
    Ok(())
}

pub fn nuke_ext4_sysfs(mnt: &str) -> anyhow::Result<()> {
    let c_mnt = std::ffi::CString::new(mnt)?;
    let mut ioctl_cmd = mysu_uapi::mysu_nuke_ext4_sysfs_cmd {
        arg: c_mnt.as_ptr() as u64,
    };
    mysuctl(mysu_uapi::MYSU_IOCTL_NUKE_EXT4_SYSFS, &raw mut ioctl_cmd)?;
    Ok(())
}

/// Wipe all entries from umount list
pub fn umount_list_wipe() -> std::io::Result<()> {
    let mut cmd = mysu_uapi::mysu_add_try_umount_cmd {
        arg: 0,
        flags: 0,
        mode: mysu_uapi::MYSU_UMOUNT_WIPE,
    };
    mysuctl(mysu_uapi::MYSU_IOCTL_ADD_TRY_UMOUNT, &raw mut cmd)?;
    Ok(())
}

/// Add mount point to umount list
pub fn umount_list_add(path: &str, flags: u32) -> anyhow::Result<()> {
    let c_path = std::ffi::CString::new(path)?;
    let mut cmd = mysu_uapi::mysu_add_try_umount_cmd {
        arg: c_path.as_ptr() as u64,
        flags,
        mode: mysu_uapi::MYSU_UMOUNT_ADD,
    };
    mysuctl(mysu_uapi::MYSU_IOCTL_ADD_TRY_UMOUNT, &raw mut cmd)?;
    Ok(())
}

/// Delete mount point from umount list
pub fn umount_list_del(path: &str) -> anyhow::Result<()> {
    let c_path = std::ffi::CString::new(path)?;
    let mut cmd = mysu_uapi::mysu_add_try_umount_cmd {
        arg: c_path.as_ptr() as u64,
        flags: 0,
        mode: mysu_uapi::MYSU_UMOUNT_DEL,
    };
    mysuctl(mysu_uapi::MYSU_IOCTL_ADD_TRY_UMOUNT, &raw mut cmd)?;
    Ok(())
}

/// Set current process's process group to init_group (pgid = 0)
pub fn set_init_pgrp() -> std::io::Result<()> {
    mysuctl(
        mysu_uapi::MYSU_IOCTL_SET_INIT_PGRP,
        std::ptr::null_mut::<u8>(),
    )?;
    Ok(())
}

pub fn set_mysu_no_new_privs() -> anyhow::Result<()> {
    let result = mysuctl(
        mysu_uapi::MYSU_IOCTL_DISABLE_ESCAPE_TO_ROOT,
        std::ptr::null_mut::<u8>(),
    )?;
    if result != 0 {
        bail!("unexpected result: {result}");
    }
    Ok(())
}

fn copy_str_to_buf(src: &str, dst: &mut [std::os::raw::c_char]) {
    for b in dst.iter_mut() {
        *b = 0;
    }
    for (s, d) in src.bytes().zip(dst.iter_mut()) {
        *d = s as std::os::raw::c_char;
    }
}

fn buf_to_string(buf: &[std::os::raw::c_char]) -> String {
    let u8_slice = unsafe { std::slice::from_raw_parts(buf.as_ptr() as *const u8, buf.len()) };
    let len = u8_slice.iter().position(|&c| c == 0).unwrap_or(u8_slice.len());
    String::from_utf8_lossy(&u8_slice[..len]).into_owned()
}

pub fn mysufs_add_sus_path(path: &str) -> anyhow::Result<()> {
    let mut cmd = mysu_uapi::st_mysufs_sus_path {
        target_pathname: [0; mysu_uapi::MYSUFS_MAX_LEN_PATHNAME as usize],
        err: 0,
    };
    copy_str_to_buf(path, &mut cmd.target_pathname);
    mysuctl(mysu_uapi::MYSU_IOCTL_MYSUFS_ADD_SUS_PATH, &raw mut cmd)?;
    if cmd.err != 0 {
        bail!("mysufs_add_sus_path failed with error: {}", cmd.err);
    }
    Ok(())
}

pub fn mysufs_add_sus_path_loop(path: &str) -> anyhow::Result<()> {
    let mut cmd = mysu_uapi::st_mysufs_sus_path {
        target_pathname: [0; mysu_uapi::MYSUFS_MAX_LEN_PATHNAME as usize],
        err: 0,
    };
    copy_str_to_buf(path, &mut cmd.target_pathname);
    mysuctl(mysu_uapi::MYSU_IOCTL_MYSUFS_ADD_SUS_PATH_LOOP, &raw mut cmd)?;
    if cmd.err != 0 {
        bail!("mysufs_add_sus_path_loop failed with error: {}", cmd.err);
    }
    Ok(())
}

pub fn mysufs_set_hide_sus_mnts(enabled: bool) -> anyhow::Result<()> {
    let mut cmd = mysu_uapi::st_mysufs_hide_sus_mnts_for_non_su_procs {
        enabled: if enabled { 1 } else { 0 },
        err: 0,
    };
    mysuctl(mysu_uapi::MYSU_IOCTL_MYSUFS_SET_HIDE_SUS_MNTS, &raw mut cmd)?;
    if cmd.err != 0 {
        bail!("mysufs_set_hide_sus_mnts failed with error: {}", cmd.err);
    }
    Ok(())
}

pub fn mysufs_add_sus_kstat(
    pathname: &str,
    target_ino: u64,
    spoofed_ino: u64,
    spoofed_dev: u64,
    spoofed_size: i64,
    flags: i32,
) -> anyhow::Result<()> {
    let mut cmd = mysu_uapi::st_mysufs_sus_kstat {
        is_statically: 0,
        target_ino,
        target_pathname: [0; mysu_uapi::MYSUFS_MAX_LEN_PATHNAME as usize],
        spoofed_ino,
        spoofed_dev,
        spoofed_nlink: 0,
        spoofed_size,
        spoofed_atime_tv_sec: 0,
        spoofed_atime_tv_nsec: 0,
        spoofed_mtime_tv_sec: 0,
        spoofed_mtime_tv_nsec: 0,
        spoofed_ctime_tv_sec: 0,
        spoofed_ctime_tv_nsec: 0,
        spoofed_blocks: 0,
        spoofed_blksize: 0,
        flags,
        err: 0,
    };
    copy_str_to_buf(pathname, &mut cmd.target_pathname);
    mysuctl(mysu_uapi::MYSU_IOCTL_MYSUFS_ADD_SUS_KSTAT, &raw mut cmd)?;
    if cmd.err != 0 {
        bail!("mysufs_add_sus_kstat failed with error: {}", cmd.err);
    }
    Ok(())
}

pub fn mysufs_update_sus_kstat(
    target_ino: u64,
    spoofed_ino: u64,
    spoofed_dev: u64,
    spoofed_size: i64,
    flags: i32,
) -> anyhow::Result<()> {
    let mut cmd = mysu_uapi::st_mysufs_sus_kstat {
        is_statically: 0,
        target_ino,
        target_pathname: [0; mysu_uapi::MYSUFS_MAX_LEN_PATHNAME as usize],
        spoofed_ino,
        spoofed_dev,
        spoofed_nlink: 0,
        spoofed_size,
        spoofed_atime_tv_sec: 0,
        spoofed_atime_tv_nsec: 0,
        spoofed_mtime_tv_sec: 0,
        spoofed_mtime_tv_nsec: 0,
        spoofed_ctime_tv_sec: 0,
        spoofed_ctime_tv_nsec: 0,
        spoofed_blocks: 0,
        spoofed_blksize: 0,
        flags,
        err: 0,
    };
    mysuctl(mysu_uapi::MYSU_IOCTL_MYSUFS_UPDATE_SUS_KSTAT, &raw mut cmd)?;
    if cmd.err != 0 {
        bail!("mysufs_update_sus_kstat failed with error: {}", cmd.err);
    }
    Ok(())
}

pub fn mysufs_set_uname(release: &str, version: &str) -> anyhow::Result<()> {
    let mut cmd = mysu_uapi::st_mysufs_uname {
        release: [0; mysu_uapi::MYSUFS_UTS_LEN as usize],
        version: [0; mysu_uapi::MYSUFS_UTS_LEN as usize],
        err: 0,
    };
    copy_str_to_buf(release, &mut cmd.release);
    copy_str_to_buf(version, &mut cmd.version);
    mysuctl(mysu_uapi::MYSU_IOCTL_MYSUFS_SET_UNAME, &raw mut cmd)?;
    if cmd.err != 0 {
        bail!("mysufs_set_uname failed with error: {}", cmd.err);
    }
    Ok(())
}

pub fn mysufs_enable_log(enabled: bool) -> anyhow::Result<()> {
    let mut cmd = mysu_uapi::st_mysufs_log {
        enabled: if enabled { 1 } else { 0 },
        err: 0,
    };
    mysuctl(mysu_uapi::MYSU_IOCTL_MYSUFS_ENABLE_LOG, &raw mut cmd)?;
    if cmd.err != 0 {
        bail!("mysufs_enable_log failed with error: {}", cmd.err);
    }
    Ok(())
}

pub fn mysufs_set_cmdline(cmdline: &str) -> anyhow::Result<()> {
    let mut cmd = mysu_uapi::st_mysufs_spoof_cmdline_or_bootconfig {
        fake_cmdline_or_bootconfig: [0; mysu_uapi::MYSUFS_FAKE_CMDLINE_OR_BOOTCONFIG_SIZE as usize],
        err: 0,
    };
    copy_str_to_buf(cmdline, &mut cmd.fake_cmdline_or_bootconfig);
    mysuctl(mysu_uapi::MYSU_IOCTL_MYSUFS_SET_CMDLINE, &raw mut cmd)?;
    if cmd.err != 0 {
        bail!("mysufs_set_cmdline failed with error: {}", cmd.err);
    }
    Ok(())
}

pub fn mysufs_add_open_redirect(target: &str, redirected: &str, uid_scheme: i32) -> anyhow::Result<()> {
    let mut cmd = mysu_uapi::st_mysufs_open_redirect {
        target_pathname: [0; mysu_uapi::MYSUFS_MAX_LEN_PATHNAME as usize],
        redirected_pathname: [0; mysu_uapi::MYSUFS_MAX_LEN_PATHNAME as usize],
        uid_scheme,
        err: 0,
    };
    copy_str_to_buf(target, &mut cmd.target_pathname);
    copy_str_to_buf(redirected, &mut cmd.redirected_pathname);
    mysuctl(mysu_uapi::MYSU_IOCTL_MYSUFS_ADD_OPEN_REDIRECT, &raw mut cmd)?;
    if cmd.err != 0 {
        bail!("mysufs_add_open_redirect failed with error: {}", cmd.err);
    }
    Ok(())
}

pub fn mysufs_add_sus_map(path: &str) -> anyhow::Result<()> {
    let mut cmd = mysu_uapi::st_mysufs_sus_map {
        target_pathname: [0; mysu_uapi::MYSUFS_MAX_LEN_PATHNAME as usize],
        err: 0,
    };
    copy_str_to_buf(path, &mut cmd.target_pathname);
    mysuctl(mysu_uapi::MYSU_IOCTL_MYSUFS_ADD_SUS_MAP, &raw mut cmd)?;
    if cmd.err != 0 {
        bail!("mysufs_add_sus_map failed with error: {}", cmd.err);
    }
    Ok(())
}

pub fn mysufs_enable_avc_log_spoofing(enabled: bool) -> anyhow::Result<()> {
    let mut cmd = mysu_uapi::st_mysufs_avc_log_spoofing {
        enabled: if enabled { 1 } else { 0 },
        err: 0,
    };
    mysuctl(mysu_uapi::MYSU_IOCTL_MYSUFS_ENABLE_AVC_LOG_SPOOFING, &raw mut cmd)?;
    if cmd.err != 0 {
        bail!("mysufs_enable_avc_log_spoofing failed with error: {}", cmd.err);
    }
    Ok(())
}

pub fn mysufs_show_version() -> anyhow::Result<String> {
    let mut cmd = mysu_uapi::st_mysufs_version {
        mysufs_version: [0; mysu_uapi::MYSUFS_MAX_VERSION_BUFSIZE as usize],
        err: 0,
    };
    mysuctl(mysu_uapi::MYSU_IOCTL_MYSUFS_SHOW_VERSION, &raw mut cmd)?;
    if cmd.err != 0 {
        bail!("mysufs_show_version failed with error: {}", cmd.err);
    }
    Ok(buf_to_string(&cmd.mysufs_version))
}

pub fn mysufs_show_enabled_features() -> anyhow::Result<String> {
    let mut cmd = mysu_uapi::st_mysufs_enabled_features {
        enabled_features: [0; mysu_uapi::MYSUFS_ENABLED_FEATURES_SIZE as usize],
        err: 0,
    };
    mysuctl(mysu_uapi::MYSU_IOCTL_MYSUFS_SHOW_ENABLED_FEATURES, &raw mut cmd)?;
    if cmd.err != 0 {
        bail!("mysufs_show_enabled_features failed with error: {}", cmd.err);
    }
    Ok(buf_to_string(&cmd.enabled_features))
}

pub fn mysufs_show_variant() -> anyhow::Result<String> {
    let mut cmd = mysu_uapi::st_mysufs_variant {
        mysufs_variant: [0; mysu_uapi::MYSUFS_MAX_VARIANT_BUFSIZE as usize],
        err: 0,
    };
    mysuctl(mysu_uapi::MYSU_IOCTL_MYSUFS_SHOW_VARIANT, &raw mut cmd)?;
    if cmd.err != 0 {
        bail!("mysufs_show_variant failed with error: {}", cmd.err);
    }
    Ok(buf_to_string(&cmd.mysufs_variant))
}
