#ifndef CONFIG_HPP
#define CONFIG_HPP

#include "config.h"
#include <cstddef>
#include <cstdint>

constexpr size_t MEM_SIZE = 0x08000000;
constexpr size_t MEM_BASE = 0x80000000;

using word_t = uint32_t;

#endif