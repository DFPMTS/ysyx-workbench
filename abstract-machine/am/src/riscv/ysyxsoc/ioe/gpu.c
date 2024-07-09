#include <am.h>
#include "ysyxsoc.h"

void __am_gpu_config(AM_GPU_CONFIG_T * cfg)
{
    *cfg = (AM_GPU_CONFIG_T) {
    .present = true, .has_accel = false,
    .width = 640, .height = 480,
    .vmemsz = 0
  };
}

void __am_gpu_status(AM_GPU_STATUS_T *status)
{
  status->ready = true;
}

int printf(char *,...);
void __am_gpu_fbdraw(AM_GPU_FBDRAW_T *ctl)
{
  uint32_t *color = (uint32_t *)(ctl->pixels);
  int w = 640;
  for (int i = 0; i < ctl->w; ++i) {
    for (int j = 0; j < ctl->h; ++j) {
      outl(VGA_BASE + ((ctl->x + i) + (ctl->y + j) * w) * 4,
           color[i + j * ctl->w]);
    }
  }
}