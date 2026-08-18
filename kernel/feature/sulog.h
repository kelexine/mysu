#ifndef __MYSU_H_SULOG
#define __MYSU_H_SULOG

#include <linux/types.h>

bool mysu_sulog_is_enabled(void);
void mysu_sulog_init(void);
void mysu_sulog_exit(void);

#endif
