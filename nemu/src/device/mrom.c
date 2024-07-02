#include <utils.h>
#include <device/map.h>

static uint8_t* mrom_base = NULL;

static void mrom_io_handler(uint32_t offset, int len, bool is_write) {  
  return;
}

void init_mrom() {
  mrom_base = new_space(CONFIG_MROM_SIZE);
  add_mmio_map("mrom", CONFIG_MROM_ADDR, mrom_base, CONFIG_MROM_SIZE, mrom_io_handler);
}