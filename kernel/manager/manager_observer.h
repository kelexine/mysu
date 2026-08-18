#ifndef __MYSU_H_MANAGER_OBSERVER
#define __MYSU_H_MANAGER_OBSERVER

#ifdef CONFIG_MYSU_DISABLE_MANAGER
static inline int mysu_observer_init(void)
{
    return 0;
}

static inline void mysu_observer_exit(void)
{
}
#else
int mysu_observer_init(void);
void mysu_observer_exit(void);
#endif

#endif // __MYSU_H_MANAGER_OBSERVER
