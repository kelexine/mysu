#ifndef __MYSU_H_APP_PROFILE
#define __MYSU_H_APP_PROFILE

#include <linux/sched.h>
#include <linux/cred.h>
#include "uapi/app_profile.h"

#define TIF_MYSU_DISABLE_ESCAPE_WITH_ROOT 63

int escape_with_root_profile(void);
void escape_to_root_for_init(void);
void setup_groups(struct root_profile *profile, struct cred *cred);

#endif
