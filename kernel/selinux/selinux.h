#ifndef __MYSU_H_SELINUX
#define __MYSU_H_SELINUX

#include <linux/types.h>
#include <linux/version.h>
#include <linux/cred.h>

#define MYSU_DOMAIN "mysu"
#define MYSU_FILE "mysu_file"

#define MYSU_CONTEXT "u:r:" MYSU_DOMAIN ":s0"
#define MYSU_FILE_CONTEXT "u:object_r:" MYSU_FILE ":s0"
#define ZYGOTE_CONTEXT "u:r:zygote:s0"
#define INIT_CONTEXT "u:r:init:s0"

void setup_selinux(const char *, struct cred *);

void setenforce(bool);

bool getenforce();

void cache_sid(void);

bool is_task_mysu_domain(const struct cred *cred);

bool is_mysu_domain();

bool is_zygote(const struct cred *cred);

bool is_init(const struct cred *cred);

void apply_mysu_rules();

int handle_sepolicy(void __user *user_data, u64 data_len);

void setup_mysu_cred();

void escape_to_root_for_adb_root();

extern u32 mysu_file_sid;
#define mysu_file_sid mysu_file_sid

#endif
