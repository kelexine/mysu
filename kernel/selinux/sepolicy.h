#ifndef __MYSU_H_SEPOLICY
#define __MYSU_H_SEPOLICY

#include <linux/types.h>

#include "ss/policydb.h"

struct selinux_policy *mysu_dup_sepolicy(struct selinux_policy *old_pol);

void mysu_destroy_sepolicy(struct selinux_policy *orig);

// Operation on types
bool mysu_type(struct policydb *db, const char *name, const char *attr);
bool mysu_attribute(struct policydb *db, const char *name);
bool mysu_permissive(struct policydb *db, const char *type);
bool mysu_enforce(struct policydb *db, const char *type);
bool mysu_typeattribute(struct policydb *db, const char *type, const char *attr);
bool mysu_exists(struct policydb *db, const char *type);

// Access vector rules
bool mysu_allow(struct policydb *db, const char *src, const char *tgt, const char *cls, const char *perm);
bool mysu_deny(struct policydb *db, const char *src, const char *tgt, const char *cls, const char *perm);
bool mysu_auditallow(struct policydb *db, const char *src, const char *tgt, const char *cls, const char *perm);
bool mysu_dontaudit(struct policydb *db, const char *src, const char *tgt, const char *cls, const char *perm);

// Extended permissions access vector rules
bool mysu_allowxperm(struct policydb *db, const char *src, const char *tgt, const char *cls, const char *range);
bool mysu_auditallowxperm(struct policydb *db, const char *src, const char *tgt, const char *cls, const char *range);
bool mysu_dontauditxperm(struct policydb *db, const char *src, const char *tgt, const char *cls, const char *range);

// Type rules
bool mysu_type_transition(struct policydb *db, const char *src, const char *tgt, const char *cls, const char *def,
                         const char *obj);
bool mysu_type_change(struct policydb *db, const char *src, const char *tgt, const char *cls, const char *def);
bool mysu_type_member(struct policydb *db, const char *src, const char *tgt, const char *cls, const char *def);

// File system labeling
bool mysu_genfscon(struct policydb *db, const char *fs_name, const char *path, const char *ctx);

#endif
