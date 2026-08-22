#ifndef __MYSU_H_UTIL
#define __MYSU_H_UTIL

#include "linux/fdtable.h" // IWYU pragma: keep
#include <linux/version.h>
#include <linux/syscalls.h>

#if LINUX_VERSION_CODE >= KERNEL_VERSION(5, 11, 0)
#define mysu_close_fd close_fd
#else
#define mysu_close_fd ksys_close
#endif

#ifndef fallthrough
#define fallthrough do {} while (0)
#endif

#ifndef TWA_RESUME
#define TWA_RESUME true
#endif

#ifndef TWA_NONE
#define TWA_NONE false
#endif

#if LINUX_VERSION_CODE < KERNEL_VERSION(5, 8, 0)
#define copy_to_kernel_nofault probe_kernel_write
#define copy_from_kernel_nofault probe_kernel_read
#define copy_from_user_nofault(dst, src, size) copy_from_user(dst, (const void __user *)src, size)
#define copy_to_user_nofault(dst, src, size) copy_to_user((void __user *)dst, src, size)
#define strncpy_from_user_nofault(dst, src, count) strncpy_from_user(dst, (const char __user *)src, count)
#endif

#if LINUX_VERSION_CODE < KERNEL_VERSION(4, 20, 0)
static inline ssize_t strscpy_pad(char *dest, const char *src, size_t count)
{
    ssize_t res = strscpy(dest, src, count);
    if (res >= 0 && (size_t)res < count)
        memset(dest + res, 0, count - res);
    return res;
}
#endif

#endif
