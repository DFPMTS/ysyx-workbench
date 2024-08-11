#include <common.h>

#if defined(MULTIPROGRAM) && !defined(TIME_SHARING)
# define MULTIPROGRAM_YIELD() yield()
#else
# define MULTIPROGRAM_YIELD()
#endif

#define NAME(key) \
  [AM_KEY_##key] = #key,

static const char *keyname[256] __attribute__((used)) = {
  [AM_KEY_NONE] = "NONE",
  AM_KEYS(NAME)
};

size_t serial_write(const void *buf, size_t offset, size_t len) {
  yield();
  for (int i = 0; i < len; ++i) {
    putch(((char *)buf)[i]);
  }
  return len;
}

size_t events_read(void *buf, size_t offset, size_t len) {
  yield();
  AM_INPUT_KEYBRD_T input;
  ioe_read(AM_INPUT_KEYBRD, &input);
  if(input.keycode == AM_KEY_NONE)
    return 0;
  int expected_to_read = snprintf(
      buf, len, "%s %s", input.keydown ? "kd" : "ku", keyname[input.keycode]);
  return expected_to_read > len ? len : expected_to_read;
}

size_t dispinfo_read(void *buf, size_t offset, size_t len) {
  AM_GPU_CONFIG_T config;
  ioe_read(AM_GPU_CONFIG, &config);
  int expect_to_read = snprintf(buf, len, "WIDTH:%d\nHEIGHT:%d\n",
                                config.width, config.height);
  return expect_to_read > len ? len : expect_to_read;
}

size_t fb_write(const void *buf, size_t offset, size_t len) {
  yield();
  AM_GPU_CONFIG_T config;
  AM_GPU_FBDRAW_T fb;
  ioe_read(AM_GPU_CONFIG, &config);
  fb.sync = 1;
  uint32_t *pixels = (uint32_t *)buf;
  fb.h = 1;
  fb.w = 1;
  fb.y = offset / config.width;
  fb.x = offset % config.width;
  fb.pixels = pixels;
  for (int i = 0; i < len; ++i){
    bool next_line = fb.x + fb.w == config.width;
    if (next_line || i == len - 1) {
      ioe_write(AM_GPU_FBDRAW, &fb);
      fb.w = 0;
      fb.x = 0;
      fb.y++;
      fb.pixels = pixels;
    }
    fb.w++;
    pixels++;    
  }
  return len;
}

void init_device() {
  Log("Initializing devices...");
  ioe_init();
}
