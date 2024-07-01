#include <utils.h>
#include <device/map.h>

static uint8_t* flash_base = NULL;

static void flash_io_handler(uint32_t offset, int len, bool is_write) {  
  return;
}

void init_flash() {
  Log("Initializing flash");
  flash_base = new_space(CONFIG_FLASH_SIZE);
  add_mmio_map("flash", CONFIG_FLASH_ADDR, flash_base, CONFIG_FLASH_SIZE, flash_io_handler);
}