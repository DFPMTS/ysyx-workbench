#include <am.h>
#include "ysyxsoc.h"

void __am_uart_rx(AM_UART_RX_T * rx) {
  while (!(inb(UART_BASE + UART_LS) & UART_LS_DR_FLAG))
    ;
  rx->data = inb(UART_BASE + UART_RX);
}

void __am_uart_tx(AM_UART_TX_T * tx) {
  putch(tx->data);
}