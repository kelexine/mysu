#ifndef __MYSU_H
#define __MYSU_H

#include <linux/types.h>
#include "uapi/mysu.h"

struct cred;
extern struct cred *mysu_cred;
extern bool mysu_late_loaded;

#endif
