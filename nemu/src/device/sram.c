#include <utils.h>
#include <device/map.h>

static uint8_t* sram_base = NULL;


static void sram_io_handler(uint32_t offset, int len, bool is_write) {
  // Nothing to do here, just return.
  return;
}

void init_sram() {
  sram_base = new_space(CONFIG_SRAM_SIZE);
  add_mmio_map("sram", CONFIG_SRAM_ADDR, sram_base, CONFIG_SRAM_SIZE, sram_io_handler);
}