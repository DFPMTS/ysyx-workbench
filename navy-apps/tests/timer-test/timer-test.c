#include <unistd.h>
#include <stdio.h>
#include <NDL.h>

int main() {
  uint32_t sec = 0;
  NDL_Init(0);
  while (1) {
    uint32_t ticks = NDL_GetTicks();
    if (ticks >= sec * 500) {
      printf("sec: %u, ticks: %u\n", sec, ticks);
      sec++;
    }
  }
}
