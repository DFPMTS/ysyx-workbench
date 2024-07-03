#ifndef MEM_HPP
#define MEM_HPP

#include "config.hpp"
#include "status.hpp"
#include <cassert>
#include <cstddef>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <iostream>
#include <vector>

using mem_word_t = uint64_t;
using vaddr_t = uint32_t;
using paddr_t = uint32_t;
#define RESET_VECTOR 0x80000000

extern uint8_t mem[MEM_SIZE];
extern uint8_t sram[SRAM_SIZE];
extern uint8_t mrom[MROM_SIZE];
extern uint8_t flash[FLASH_SIZE];
extern uint8_t psram[PSRAM_SIZE];
extern bool access_device;

void load_img(const char *img);

#endif