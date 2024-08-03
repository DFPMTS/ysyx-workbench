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
    default: panic("Unhandled syscall ID = %d", a[0]);
  }
  c->GPRx = retval;
}
