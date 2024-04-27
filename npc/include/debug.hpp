#ifndef DEBUG_HPP
#define DEBUG_HPP

#include <cstdio>

extern FILE *log_fd;

#define _Log(...)                                                              \
  do {                                                                         \
    log_write(__VA_ARGS__);                                                    \
    printf(__VA_ARGS__);                                                       \
  } while (0)

#define log_write(...)                                                         \
  do {                                                                         \
    fprintf(log_fd, __VA_ARGS__);                                              \
  } while (0)

#define Log(format, ...)                                                       \
  _Log("[%s:%d %s] " format "\n", (__FILE__ + 38), __LINE__, __func__,         \
       ##__VA_ARGS__)

#define Assert(cond, ...)                                                      \
  do {                                                                         \
  } while (0)

void init_log(const char *log_file);

#endif