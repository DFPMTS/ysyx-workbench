#include <am.h>
#include <klib.h>
#include <klib-macros.h>

// for now (RV32) the alignment is 8-byte
#define ALIGN 8
#define ALIGN_MASK 0x7

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)
static unsigned long int next = 1;

int rand(void) {
  // RAND_MAX assumed to be 32767
  next = next * 1103515245 + 12345;
  return (unsigned int)(next/65536) % 32768;
}

void srand(unsigned int seed) {
  next = seed;
}

int abs(int x) {
  return (x < 0 ? -x : x);
}

int atoi(const char* nptr) {
  int x = 0;
  while (*nptr == ' ') { nptr ++; }
  while (*nptr >= '0' && *nptr <= '9') {
    x = x * 10 + *nptr - '0';
    nptr ++;
  }
  return x;
}

void *malloc(size_t size) {
  // On native, malloc() will be called during initializaion of C runtime.
  // Therefore do not call panic() here, else it will yield a dead recursion:
  //   panic() -> putchar() -> (glibc) -> malloc() -> panic()
// #if !(defined(__ISA_NATIVE__) && defined(__NATIVE_USE_KLIB__))
//   panic("Not implemented");
// #endif
  static uint32_t addr = -1;
  // avoid touching _heap_start directly
  if (addr == -1) {
    addr = (uint32_t)heap.start;
  }

  // move to next 8-byte boundry
  uint32_t ptr = (addr + (ALIGN - 1)) & ~ALIGN_MASK;
  // calculate next_addr
  uint32_t next_addr = ptr + size;

  if(next_addr > (uint32_t)heap.end){
    return NULL;
  }

  addr = next_addr;
  return (void *)ptr;
}

void free(void *ptr) {
}

#endif
