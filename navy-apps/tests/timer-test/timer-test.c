#include <unistd.h>
#include <stdio.h>
#include <sys/time.h>

int main() {
  struct timeval tv;
  struct timezone tz;
  long sec = 0;
  while (1) {
    gettimeofday(&tv,&tz);
    if(tv.tv_usec + tv.tv_sec * 1000000 >= sec * 500000){
        printf("sec: %ld, us: %ld\n", tv.tv_sec, tv.tv_usec);
        sec++;
    }
  }
}
