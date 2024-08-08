#include <stdint.h>
#include <stdlib.h>
#include <assert.h>

int main(int argc, char *argv[], char *envp[]);
extern char **environ;
void call_main(uintptr_t *args) {
  printf("args: %p\n",args);
  char *empty[] =  {NULL };
  environ = empty;
  int argc = *(int *)args;
  char *argv = args + 1;
  char *envp = args + 1 + argc;

  exit(main(argc, argv, envp));
  assert(0);
}
