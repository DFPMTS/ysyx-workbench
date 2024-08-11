#include "syscall.h"
#include <common.h>
#include <fs.h>
#include <proc.h>

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

void switch_boot_pcb();

int context_uload(PCB *pcb, const char *filename, char *const argv[],
                   char *const envp[]);

int mm_brk(uintptr_t brk);

void do_syscall(Context *c) {
  uintptr_t a[4], retval = 0;
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
      // halt(c->GPR2);
      context_uload(current, "/bin/nterm", (char *[]){"/bin/nterm", NULL},
                    (char *[]){NULL});
      switch_boot_pcb();
      yield();
      // halt(c->GPR2);
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
      retval = mm_brk(a[1]);
      Trace("%s(%p) = %d", syscall_name, a[1], retval);
      break;

    case SYS_execve:
      /*
      因此Nanos-lite在处理SYS_execve系统调用的时候就需要检查将要执行的程序是否存在,
      如果不存在, 就需要返回一个错误码. 我们可以通过fs_open()来进行检查,
      如果需要打开的文件不存在, 就返回一个错误的值, 此时SYS_execve返回-2.
      另一方面, libos中的execve()还需要检查系统调用的返回值:
      如果系统调用的返回值小于0, 则通常表示系统调用失败,
      此时需要将系统调用返回值取负, 作为失败原因设置到一个全局的外部变量errno中,
      然后返回-1.
      */
      retval = 0;
      Trace("%s(\"%s\", %p, %p)", syscall_name, (char *)a[1], a[2], a[3], retval);
      retval = context_uload(current, (char *)a[1], (char *const *)a[2], (char *const *)a[3]);
      if (!retval) {
        switch_boot_pcb();
        yield();
      }
      break;

    case SYS_gettimeofday:
      retval = gettimeofday((struct timeval *)a[1], (void *)a[2]);
      break;

    default: panic("Unhandled syscall ID = %d", a[0]);
  }
  c->GPRx = retval;
}
