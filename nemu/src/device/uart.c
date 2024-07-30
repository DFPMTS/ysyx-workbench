/***************************************************************************************
* Copyright (c) 2014-2022 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

#include <utils.h>
#include <device/map.h>

/* http://en.wikibooks.org/wiki/Serial_Programming/8250_UART_Programming */
// NOTE: this is compatible to 16550

static uint8_t *uart_base = NULL;

#define UART_TX  0
#define UART_RX  0
#define UART_DL1 0
#define UART_DL2 1
#define UART_LC 3
#define UART_LC_DL_FLAG (1 << 7)
#define UART_LS 5
#define UART_LS_DR_FLAG (1 << 0)
#define UART_LS_TFE_FLAG (1 << 5)

#define UART_DL_ACCESS (uart_base[UART_LC] & UART_LC_DL_FLAG)

static void uart_putc(char ch) {
  MUXDEF(CONFIG_TARGET_AM, putch(ch), putc(ch, stderr));
}

static void uart_io_handler(uint32_t offset, int len, bool is_write) {
  assert(len == 1);
  switch (offset) {
    /* We bind the serial port with the host stderr in NEMU. */
    case UART_DL1: 
      // also UART_TX/RX
      if (is_write) {
        if (!UART_DL_ACCESS)
          uart_putc(uart_base[UART_TX]);
      } else
        panic("do not support read");
      break;
    case UART_DL2: 
      break;
    case UART_LC:
      // nothing to do here 
      break;
    case UART_LS:
      uart_base[UART_LS] = UART_LS_TFE_FLAG;
      break;
    default: panic("do not support offset = %d", offset);
  }
}

void init_uart() {
  uart_base = new_space(6);
  add_mmio_map("uart", CONFIG_UART_ADDR, uart_base, 8, uart_io_handler);
}
