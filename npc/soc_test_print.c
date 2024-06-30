#define UART_BASE 0x10000000
#define UART_TX   0
void _start() {
  for (int i=0;i<120;++i)
    *(volatile char *)(UART_BASE + UART_TX) = 'A';  
  // *(volatile char *)(UART_BASE + UART_TX) = '\n';
  while (1);
}