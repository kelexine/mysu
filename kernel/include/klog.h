#ifndef __MYSU_H_KLOG
#define __MYSU_H_KLOG

#include <linux/printk.h>

#ifdef pr_fmt
#undef pr_fmt
#define pr_fmt(fmt) "MySU: " fmt
#endif

#endif
