#include <unistd.h>
#include <stdio.h>

int main(int argc, char *argv[], char *envp[]) {
  write(1, "Hello World!\n", 13);
  int i = 2;
  volatile int j = 0;
  while (1) {
    j ++;
    if (j == 10000) {
      printf("argc: %d\n",argc);
      printf("argv: %p\n",argv);
      for (int i = 0; i < argc; ++i) {
        printf("argv[%d]: %p - %s\n", i, argv[i], argv[i]);
      }
      for (int i = 0; envp[i]; ++i) {
        printf("envp[%d]: %s\n", i, envp[i]);
      }
      j = 0;
    }
  }
  return 0;
}
