#ifndef __MYSU_H_FEATURE
#define __MYSU_H_FEATURE

#include <linux/types.h>
#include "uapi/feature.h" // IWYU pragma: keep

typedef int (*mysu_feature_get_t)(u64 *value);
typedef int (*mysu_feature_set_t)(u64 value);

struct mysu_feature_handler {
    u32 feature_id;
    const char *name;
    mysu_feature_get_t get_handler;
    mysu_feature_set_t set_handler;
};

int mysu_register_feature_handler(const struct mysu_feature_handler *handler);

int mysu_unregister_feature_handler(u32 feature_id);

int mysu_get_feature(u32 feature_id, u64 *value, bool *supported);

int mysu_set_feature(u32 feature_id, u64 value);

void mysu_feature_init(void);

void mysu_feature_exit(void);

#endif // __MYSU_H_FEATURE
