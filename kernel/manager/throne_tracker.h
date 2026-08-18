#ifndef __MYSU_H_UID_OBSERVER
#define __MYSU_H_UID_OBSERVER

#include <linux/types.h>
#ifdef CONFIG_MYSU_DISABLE_MANAGER
static inline void mysu_throne_tracker_init()
{
}

static inline void mysu_throne_tracker_exit()
{
}

static inline void track_throne(bool prune_only)
{
    (void)prune_only;
}
#else
void mysu_throne_tracker_init();

void mysu_throne_tracker_exit();

void track_throne(bool prune_only);
#endif

#endif
