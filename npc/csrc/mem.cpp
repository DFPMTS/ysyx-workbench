#include "mem.hpp"
#include "cpu.hpp"
#include "debug.hpp"
#include <cassert>
#include <cstring>
#include <ctime>
#include <iostream>

#define OFFSET 0x80000000
#define SIZE 0x08000000

#define DEVICE_BASE 0xa0000000

#define RTC_ADDR (DEVICE_BASE + 0x0000048)
#define SERIAL_PORT (DEVICE_BASE + 0x00003f8)

static uint32_t image[128] = {
    0x00000297, // auipc t0,0
    0x00028823, // sb  zero,16(t0)
    0x0102c503, // lbu a0,16(t0)
    0x00100073, // ebreak (used as nemu_trap)
    0xdeadbeef, // some data
};

bool access_device = false;
uint8_t mem[MEM_SIZE];
#define ADDR_MASK (~0x3u)
#define BYTE_MASK (0xFFu)

static bool in_pmem(paddr_t addr) { return addr - MEM_BASE < MEM_SIZE; }
static bool in_clock(paddr_t addr) {
  return addr == RTC_ADDR || addr == RTC_ADDR + 4;
}
static bool in_serial(paddr_t addr) { return addr == SERIAL_PORT; }

static uint8_t *guest_to_host(paddr_t addr) { return mem + addr - MEM_BASE; }
static word_t clock_read(paddr_t offset) {
  assert(offset == 0 || offset == 4);
  static uint64_t boot_time = 0;

  struct timespec now;
  clock_gettime(CLOCK_MONOTONIC_COARSE, &now);
  uint64_t now_time = now.tv_sec * 1000000 + now.tv_nsec / 1000;

  if (boot_time == 0) {
    boot_time = now_time;
  }
  now_time -= boot_time;

  if (offset == 0)
    return (uint32_t)now_time;
  if (offset == 4)
    return now_time >> 32;
}
void serial_write(paddr_t offset, word_t wdata) {
  assert(offset == 0);
  putc(wdata & BYTE_MASK, stderr);
}

void load_img(const char *img) {
  FILE *fd = NULL;
  if (img) {
    fd = fopen(img, "rb");
  }
  bool succ = true;
  if (!img) {
    printf("No img provided, using built-in one.\n");
    succ = false;
  } else if (!fd) {
    printf("Unable to open img: %s\n", img);
    succ = false;
  }

  if (succ) {
    fseek(fd, 0, SEEK_END);
    auto size = ftell(fd);
    fseek(fd, 0, SEEK_SET);
    assert(fread(mem, 1, size, fd) == size);
  } else {
    memcpy(mem, image, sizeof(image));
  }
}

word_t host_read(uint8_t *addr) {
  auto retval = *(uint32_t *)(addr);
  return retval;
}
void host_write(uint8_t *addr, word_t wdata, unsigned char wmask) {
  uint8_t *data = (uint8_t *)&wdata;
  for (int i = 0; i < 4; ++i) {
    if ((wmask >> i) & 1) {
      addr[i] = data[i];
    }
  }
}

extern "C" {
word_t mem_read(paddr_t addr) {
  addr &= ADDR_MASK;
#ifdef MTRACE
  log_write("(%lu)read:  0x%08x : ", eval_time, addr);
#endif
  bool valid = false;
  word_t retval = 0;
  if (in_pmem(addr)) {
    valid = true;
    retval = host_read(guest_to_host(addr));
  }
  if (in_clock(addr)) {
    access_device = true;
#ifdef MTRACE
    log_write("|clock| %u", addr);
#endif
    valid = true;
    retval = clock_read(addr - RTC_ADDR);
  }
#ifdef MTRACE
  if (valid)
    log_write("0x%08x / %u\n", retval, retval);
  else
    log_write("NOT VALID / NOT VALID\n");
#endif
  if (valid) {
    return retval;
  }
  if (running)
    assert(0);
  return 0;
}

void mem_write(paddr_t addr, word_t wdata, unsigned char wmask) {
  if (!running)
    return;
  addr &= ADDR_MASK;
#ifdef MTRACE
  log_write("(%lu)write: 0x%08x - %x : 0x%08x / %u\n", eval_time, addr, wmask,
            wdata, wdata);
#endif
  if (in_pmem(addr)) {
    host_write(guest_to_host(addr), wdata, wmask);
    return;
  }
  if (in_serial(addr) && wmask == 1) {
    access_device = true;
#ifdef MTRACE
    log_write("|serial|\n");
#endif
    serial_write(addr - SERIAL_PORT, wdata);
    return;
  }
  assert(0);
}

word_t inst_fetch(paddr_t pc) { return mem_read(pc); }

void nemu_break() { running = 0; }
}