#include <common.h>
#include <isa-def.h>
#include "local-include/reg.h"
#include <isa.h>
#include <cpu/cpu.h>

static word_t mvenderid = 0x79737978;
static word_t marchid = 23060238;

word_t *get_csr(word_t csr_addr) {
  switch (csr_addr) {
  case 0x180:
    return &cpu.satp;
    break;
  case 0x300: // mstatus
    return &cpu.mstatus;
    break;
  case 0x305: // mtvec
    return &cpu.mtvec;
    break;
  case 0x341: // mepc
    return &cpu.mepc;
    break;
  case 0x342: // mcause
    return &cpu.mcause;
    break;
  case 0xf11: // mvendorid
    return &mvenderid;
    break;  
  case 0xf12: // marchid
    return &marchid;
    break;
  default:
    panic("Invalid csr address: %u\n", csr_addr);
    break;
  }
}

word_t csrrw(word_t csr_addr, uint32_t value) {
  word_t *csr = get_csr(csr_addr);
  word_t retval = *csr;
  *csr = value;
  return retval;
}


word_t csrrs(word_t csr_addr, uint32_t value) {
  word_t *csr = get_csr(csr_addr);
  word_t retval = *csr;
  *csr |= value;
  return retval;
}
