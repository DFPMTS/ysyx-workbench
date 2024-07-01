#include <am.h>
#include <klib-macros.h>
#include "ysyxsoc.h"

extern char _heap_start;
int main(const char *args);

extern char _pmem_start;
#define PMEM_SIZE (128 * 1024 * 1024)
#define PMEM_END  ((uintptr_t)&_pmem_start + PMEM_SIZE)

Area heap = RANGE(&_heap_start, PMEM_END);
#ifndef MAINARGS
#define MAINARGS ""
#endif
static const char mainargs[] = MAINARGS;

void putch(char ch) { 
  outb(UART_BASE + UART_TX, ch); 
}

void halt(int code) {
  asm volatile("mv a0, %0; ebreak" : :"r"(code));
  while (1);
}

void _trm_init() {
  extern char _data_lma_start, _data_lma_end, _data_vma_start;
  char *data_lma = &_data_lma_start, *data_vma = &_data_vma_start;
  char *data_lma_end = &_data_lma_end;
  while (data_lma < data_lma_end) {
    *data_vma = *data_lma;
    data_lma ++, data_vma ++;
  }

  extern char _bss_start, _bss_end;
  char *bss = &_bss_start;
  char *bss_end = &_bss_end;
  while (bss < bss_end) {
    *bss = 0;
    bss ++;
  }

  int ret = main(mainargs);
  halt(ret);
}
