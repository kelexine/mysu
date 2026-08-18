#ifndef __MYSU_SYMBOL_RESOLVER_H
#define __MYSU_SYMBOL_RESOLVER_H

void *mysu_resolve_symbol_for_functable_hook(const char *symbol_name);
unsigned long find_kernel_symbol_exact(const char *symbol_name);
void mysu_init_symbol_resolver();

#endif
