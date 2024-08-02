/***************************************************************************************
* Copyright (c) 2014-2022 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

#include <common.h>
#include <device/map.h>
#include <SDL2/SDL.h>

enum {
  reg_freq,
  reg_channels,
  reg_samples,
  reg_sbuf_size,
  reg_init,
  reg_count,
  reg_iter,
  reg_lock,
  nr_reg
};

static uint8_t *sbuf = NULL;
static uint32_t *audio_base = NULL;

#define AUDIO_FREQ_OFFSET               0x00
#define AUDIO_CHANNELS_OFFSET           0x04
#define AUDIO_SAMPLES_OFFSET            0x08
#define AUDIO_SBUF_SIZE_OFFSET          0x0c
#define AUDIO_INIT_OFFSET               0x10
#define AUDIO_COUNT_OFFSET              0x14
#define AUDIO_ITER_OFFSET               0x18
#define AUDIO_LOCK_OFFSET               0x1c

static void sdl_audio_callback(void *userdata, uint8_t *stream, int len) {
  // advance = min(len, AUDIO_COUNT)
  int advance = (len > audio_base[reg_count]) ? audio_base[reg_count] : len;
  // copy data from sbuf to stream
  memcpy(stream, sbuf + audio_base[reg_iter], advance);
  // fill zero
  memset(stream + advance, 0, len - advance);

  // update iter
  audio_base[reg_iter] = audio_base[reg_iter] + advance;
  if (audio_base[reg_iter] >= CONFIG_SB_SIZE) {
    audio_base[reg_iter] -= CONFIG_SB_SIZE;
  }

  // update count
  audio_base[reg_count] -= advance;
}

static void sdl_audio_init()
{
  SDL_AudioSpec spec;
  spec.channels = audio_base[reg_channels];
  spec.freq = audio_base[reg_freq];
  spec.samples = audio_base[reg_samples];
  spec.format = AUDIO_S16SYS;
  spec.userdata = NULL;
  spec.callback = sdl_audio_callback;
  SDL_InitSubSystem(SDL_INIT_AUDIO);
  SDL_OpenAudio(&spec, NULL);
  SDL_PauseAudio(0);
}

static void audio_io_handler(uint32_t offset, int len, bool is_write) {
  switch (offset) {
  case AUDIO_FREQ_OFFSET:
  case AUDIO_CHANNELS_OFFSET:
  case AUDIO_SAMPLES_OFFSET:
  case AUDIO_COUNT_OFFSET:
    // nothing to do here
    break;

  case AUDIO_INIT_OFFSET:
    if (is_write) {
      if (audio_base[reg_init]) {
        sdl_audio_init();
        audio_base[reg_init] = 0;
      }
    }
    break;

  case AUDIO_SBUF_SIZE_OFFSET:
    if (is_write) {
      panic("audio: AUDIO_SBUF_SIZE is read-only");
    } else {
      audio_base[reg_sbuf_size] = CONFIG_SB_SIZE;
    }
    break;

  case AUDIO_ITER_OFFSET:
    if (is_write) {
      panic("audio: AUDIO_ITER is read-only");
    }
    break;

  case AUDIO_LOCK_OFFSET:
    if (is_write) {
      if (audio_base[reg_lock]) {
        SDL_LockAudio();
      } else {
        SDL_UnlockAudio();
      }
      break;
    }
  default:
    break;
  }
}

void init_audio() {
  uint32_t space_size = sizeof(uint32_t) * nr_reg;
  audio_base = (uint32_t *)new_space(space_size);
#ifdef CONFIG_HAS_PORT_IO
  add_pio_map ("audio", CONFIG_AUDIO_CTL_PORT, audio_base, space_size, audio_io_handler);
#else
  add_mmio_map("audio", CONFIG_AUDIO_CTL_MMIO, audio_base, space_size, audio_io_handler);
#endif

  // AUDIO_COUNT must be cleared to 0
  audio_base[reg_count] = 0;

  sbuf = (uint8_t *)new_space(CONFIG_SB_SIZE);
  add_mmio_map("audio-sbuf", CONFIG_SB_ADDR, sbuf, CONFIG_SB_SIZE, NULL);
}
