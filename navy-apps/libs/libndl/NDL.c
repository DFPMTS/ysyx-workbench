#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <assert.h>
#include <fcntl.h>
#include <sys/time.h>

static int evtdev = -1;
static int fbdev = -1;
static int screen_w = 0, screen_h = 0;

uint32_t NDL_GetTicks() {
  struct timeval tv;
  gettimeofday(&tv, NULL);
  return tv.tv_sec * 1000 + tv.tv_usec / 1000;
}

int NDL_PollEvent(char *buf, int len) {
  int events_fd = open("/dev/events", 0);
  assert(events_fd != -1);
  return read(events_fd, buf, len) != 0;
}

void NDL_OpenCanvas(int *w, int *h) {
  if (getenv("NWM_APP")) {
    int fbctl = 4;
    fbdev = 5;
    screen_w = *w; screen_h = *h;
    char buf[64];
    int len = sprintf(buf, "%d %d", screen_w, screen_h);
    // let NWM resize the window and create the frame buffer
    write(fbctl, buf, len);
    while (1) {
      // 3 = evtdev
      int nread = read(3, buf, sizeof(buf) - 1);
      if (nread <= 0) continue;
      buf[nread] = '\0';
      if (strcmp(buf, "mmap ok") == 0) break;
    }
    close(fbctl);
  } else {
    if (*w == 0 && *h == 0) {
      char buf[64];
      int fd = open("/proc/dispinfo", 0);
      read(fd, buf, sizeof(buf));
      sscanf(buf, "WIDTH : %d HEIGHT : %d", w, h);
    } else {
      screen_w = *w;
      screen_h = *h;
    }
  }
}

void NDL_DrawRect(uint32_t *pixels, int x, int y, int w, int h) {
  char buf[64];
  int real_w, real_h;
  int fd = open("/proc/dispinfo", 0);
  read(fd, buf, sizeof(buf));
  sscanf(buf, "WIDTH : %d HEIGHT : %d", &real_w, &real_h);
  int offset_x = (real_w - screen_w) / 2, offset_y = (real_h - screen_h) / 2;
  fd = open("/dev/fb", 0);
  for (int i = 0; i < h; ++i) {
    lseek(fd, (offset_y + y + i) * real_w + (offset_x + x), SEEK_SET);
    write(fd, &pixels[i * w], w);
  }
}

void NDL_OpenAudio(int freq, int channels, int samples) {
}

void NDL_CloseAudio() {
}

int NDL_PlayAudio(void *buf, int len) {
  return 0;
}

int NDL_QueryAudio() {
  return 0;
}

int NDL_Init(uint32_t flags) {
  if (getenv("NWM_APP")) {
    evtdev = 3;
  }
  return 0;
}

void NDL_Quit() {
}
