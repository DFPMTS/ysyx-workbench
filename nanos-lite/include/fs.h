#ifndef __FS_H__
#define __FS_H__

#include <common.h>

#ifndef SEEK_SET
enum {SEEK_SET, SEEK_CUR, SEEK_END};
#endif

int fs_open(const char *pathname);
size_t fs_read(int fd, void *buf, size_t count);
size_t fs_write(int fd, const void *buf, size_t count);
size_t fs_lseek(int fd, size_t offset, int whence);
int fs_close(int fd);

const char* fs_getfilename(int fd);

#endif
