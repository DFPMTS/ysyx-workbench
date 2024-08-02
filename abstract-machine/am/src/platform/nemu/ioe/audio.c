#include <am.h>
#include <nemu.h>
#include <stdio.h>

#define AUDIO_FREQ_ADDR      (AUDIO_ADDR + 0x00)
#define AUDIO_CHANNELS_ADDR  (AUDIO_ADDR + 0x04)
#define AUDIO_SAMPLES_ADDR   (AUDIO_ADDR + 0x08)
#define AUDIO_SBUF_SIZE_ADDR (AUDIO_ADDR + 0x0c)
#define AUDIO_INIT_ADDR      (AUDIO_ADDR + 0x10)
#define AUDIO_COUNT_ADDR     (AUDIO_ADDR + 0x14)
// AUDIO_ITER gives the current read pointer in sbuf
// write pointer can be calculated by (AUDIO_ITER + AUDIO_COUNT) % AUDIO_SBUF_SIZE
#define AUDIO_ITER_ADDR      (AUDIO_ADDR + 0x18)
// write 1 to AUDIO_LOCK to lock, write 0 to unlock
#define AUDIO_LOCK_ADDR      (AUDIO_ADDR + 0x1c)

void __am_audio_init() {
}

void __am_audio_config(AM_AUDIO_CONFIG_T *cfg) {
  cfg->present = true;
  cfg->bufsize = inl(AUDIO_SBUF_SIZE_ADDR);
}

void __am_audio_ctrl(AM_AUDIO_CTRL_T *ctrl) {
  outl(AUDIO_FREQ_ADDR, ctrl->freq);
  outl(AUDIO_SAMPLES_ADDR,ctrl->samples);
  outl(AUDIO_CHANNELS_ADDR, ctrl->channels);
  outl(AUDIO_INIT_ADDR, 1);
}

void __am_audio_status(AM_AUDIO_STATUS_T *stat) {
  stat->count = inl(AUDIO_COUNT_ADDR);
}

void __am_audio_play(AM_AUDIO_PLAY_T *ctl) {
  int len = ctl->buf.end - ctl->buf.start;
  int sbuf_size = inl(AUDIO_SBUF_SIZE_ADDR);
  // wait until there is enough space in the sbuf
  while (inl(AUDIO_COUNT_ADDR) + len > sbuf_size)
    ;

  // lock the audio device
  outl(AUDIO_LOCK_ADDR, 1);

  // write the data to the sbuf, iter points to end of the buffer
  uint32_t iter = inl(AUDIO_ITER_ADDR) + inl(AUDIO_COUNT_ADDR);
  if(iter >= sbuf_size) iter -= sbuf_size;
  for (int i = 0; i < len; i++) {
    outb(AUDIO_SBUF_ADDR + iter, ((uint8_t *)ctl->buf.start)[i]);
    iter = (iter + 1 == sbuf_size) ? 0 : iter + 1;
  }
  // update count
  outl(AUDIO_COUNT_ADDR, inl(AUDIO_COUNT_ADDR) + len);

  // unlock
  outl(AUDIO_LOCK_ADDR, 0);
}
