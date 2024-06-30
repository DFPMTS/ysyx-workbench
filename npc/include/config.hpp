#ifndef CONFIG_HPP
#define CONFIG_HPP

#include "config.h"
#include <cstddef>
#include <cstdint>

constexpr size_t MEM_SIZE = 0x08000000;
constexpr size_t MEM_BASE = 0x80000000;

constexpr size_t SRAM_SIZE = 0x00008000;
constexpr size_t SRAM_BASE = 0x08000000;

constexpr size_t MROM_SIZE = 0x00001000;
constexpr size_t MROM_BASE = 0x20000000;

using word_t = uint32_t;

#endif