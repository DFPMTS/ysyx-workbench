#include <common.h>
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

void do_syscall(Context *c) {
  uintptr_t a[4], retval;
  a[0] = c->GPR1;
  a[1] = c->GPR2;
  a[2] = c->GPR3;
  a[3] = c->GPR4;
  char *syscall_name = syscall_name_table[a[0]];
  switch (a[0]) {
    case SYS_exit:
      Log("%s(%d)",syscall_name, a[1]);
      halt(c->GPR2);
      break;
    case SYS_yield:      
      yield();      
      retval = 0;
      Log("%s() = %d",syscall_name, retval);
      break;    
    case SYS_write:
      // fd: a[1], buf: a[2], len: a[3]
      // printf("fd: %d buf: %x len: %d\n",a[1],a[2],a[3]);
      if (a[1] == 1) {
        // stdout
        for (int i = 0; i < a[3]; ++i) {
          putch(((char *)a[2])[i]);
        }
        retval = a[3];
      } else {
        panic("SYS_write only support fd=1");
      }
      Log("%s(%d, %x, %u) = %d", syscall_name, a[1], a[2], a[3], retval);
      break;
    default: panic("Unhandled syscall ID = %d", a[0]);
  }
  c->GPRx = retval;
}
