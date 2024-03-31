#include <am.h>
#include <nemu.h>

#define SYNC_ADDR (VGACTL_ADDR + 4)

void __am_gpu_init() {
}

void __am_gpu_config(AM_GPU_CONFIG_T *cfg) {
  *cfg = (AM_GPU_CONFIG_T) {
    .present = true, .has_accel = false,
    .width = inw(VGACTL_ADDR + 2), .height = inw(VGACTL_ADDR),
    .vmemsz = 0
  };
}

void __am_gpu_fbdraw(AM_GPU_FBDRAW_T *ctl) {
  uint32_t *color = (uint32_t *)(ctl->pixels);
  int w = inw(VGACTL_ADDR + 2);
  for (int i = 0; i < ctl->w; ++i) {
    for (int j = 0; j < ctl->h; ++j) {
      outl(FB_ADDR + ((ctl->x + i) + (ctl->y + j) * w) * 4,
           color[i + j * ctl->w]);
    }
  }

  if (ctl->sync) {
    outl(SYNC_ADDR, 1);
  }
}

void __am_gpu_status(AM_GPU_STATUS_T *status) {
  status->ready = true;
}
