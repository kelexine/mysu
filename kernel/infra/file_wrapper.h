#ifndef MYSU_FILE_WRAPPER_H
#define MYSU_FILE_WRAPPER_H

#include <linux/file.h>
#include <linux/fs.h>

int mysu_install_file_wrapper(int fd);
void mysu_file_wrapper_init(void);

#endif // MYSU_FILE_WRAPPER_H
