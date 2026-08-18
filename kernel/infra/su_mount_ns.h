#ifndef __MYSU_SU_MOUNT_NS_H
#define __MYSU_SU_MOUNT_NS_H

#include <linux/types.h>

#define MYSU_NS_INHERITED 0
#define MYSU_NS_GLOBAL 1
#define MYSU_NS_INDIVIDUAL 2

struct mysu_mns_tw {
    struct callback_head cb;
    int32_t ns_mode;
};

void setup_mount_ns(int32_t ns_mode);

#endif
