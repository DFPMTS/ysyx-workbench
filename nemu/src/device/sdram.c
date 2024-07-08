#include <utils.h>
#include <device/map.h>

static uint8_t* sdram_base = NULL;


static void sdram_io_handler(uint32_t offset, int len, bool is_write) {
  // Nothing to do here, just return.
  return;
}

void init_sdram() {
  sdram_base = new_space(CONFIG_SDRAM_SIZE);
  add_mmio_map("sdram", CONFIG_SDRAM_ADDR, sdram_base, CONFIG_SDRAM_SIZE, sdram_io_handler);
}