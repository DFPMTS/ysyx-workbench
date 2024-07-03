#include <am.h>
#include <klib-macros.h>
#include "ysyxsoc.h"

extern char _heap_start, _heap_end;
int main(const char *args);

Area heap = RANGE(&_heap_start, &_heap_end);
#ifndef MAINARGS
#define MAINARGS ""
#endif
static const char mainargs[] = MAINARGS;

void putch(char ch) {
  while (!(inb(UART_BASE + UART_LS) & UART_LS_TFE_FLAG))
    ;
  outb(UART_BASE + UART_TX, ch); 
}

void halt(int code) {
  asm volatile("mv a0, %0; ebreak" : :"r"(code));
  while (1);
}

__attribute_maybe_unused__ static void uart_init() 
{
  int8_t LC_old = inb(UART_BASE + UART_LC);
  // the LC_DL bits should be 0 after reset
  outb(UART_BASE + UART_LC, LC_old | UART_LC_DL_FLAG);
  outb(UART_BASE + UART_DL2, 0);
  outb(UART_BASE + UART_DL1, 1);
  outb(UART_BASE + UART_LC, LC_old);
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

  // uart_init();

  int ret = main(mainargs);
  halt(ret);
}
