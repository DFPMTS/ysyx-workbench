#include <common.h>
#include <fs.h>
#include "syscall.h"

char *syscall_name_table[] = {
  "exit",
  "yield",
  "open",
  "read",
  "write",
  "kill",
  "getpid",
  "close",
  "lseek",
  "brk",
  "fstat",
  "time",
  "signal",
  "execve",
  "fork",
  "link",
  "unlink",
  "wait",
  "times",
  "gettimeofday"
};

struct timeval {
  long tv_sec;  /* seconds */
  long tv_usec; /* and microseconds */
};

int gettimeofday(struct timeval *tv, void *tz) {
  AM_TIMER_UPTIME_T uptime;
  ioe_read(AM_TIMER_UPTIME, &uptime);
  // printf("usec: %lu\n",uptime.us);
  tv->tv_sec = uptime.us / 1000000;
  tv->tv_usec = uptime.us % 1000000;
  return 0;
}

void do_syscall(Context *c) {
  uintptr_t a[4], retval;
  a[0] = c->GPR1;
  a[1] = c->GPR2;
  a[2] = c->GPR3;
  a[3] = c->GPR4;
  char *syscall_name = syscall_name_table[a[0]];
  (void) syscall_name;
  // printf("%s\n",syscall_name);
  switch (a[0]) {
    case SYS_exit:
      Trace("%s(%d)",syscall_name, a[1]);
      halt(c->GPR2);
      break;

    case SYS_yield:
      Trace("%s()", syscall_name);
      yield();      
      retval = 0;      
      break;

    case SYS_open:
      // pathname: a[1]
      retval = fs_open((char *)a[1]);
      Trace("%s(\"%s\") = %d", syscall_name, (char *)a[1], retval);
      break;

    case SYS_read:
      // fd: a[1], buf: a[2], len: a[3]
      retval = fs_read(a[1], (void *)a[2], a[3]);
      Trace("%s(\"%s\", %p, %u) = %d", syscall_name, fs_getfilename(a[1]), a[2], a[3], retval);
      break;    

    case SYS_write:
      // fd: a[1], buf: a[2], len: a[3]
      retval = fs_write(a[1], (void *)a[2], a[3]);
      Trace("%s(\"%s\", %p, %u) = %d", syscall_name, fs_getfilename(a[1]), a[2], a[3], retval);
      break;

    case SYS_close:
      // fd: a[1]
      retval = fs_close(a[1]);
      Trace("%s(\"%s\") = %d", syscall_name, fs_getfilename(a[1]), retval);
      break;

    case SYS_lseek:
      // fd: a[1], offset: a[2], whence: a[3]
      retval = fs_lseek(a[1],a[2],a[3]);
      Trace("%s(\"%s\", %u, %d) = %d", syscall_name, fs_getfilename(a[1]), a[2], a[3], retval);
      break;

    case SYS_brk:
      retval = 0;
      Trace("%s(%p) = %d", syscall_name, a[1], retval);
      break;

    case SYS_gettimeofday:
      retval = gettimeofday((struct timeval *)a[1], (void *)a[2]);
      break;

    default: panic("Unhandled syscall ID = %d", a[0]);
  }
  c->GPRx = retval;
}
