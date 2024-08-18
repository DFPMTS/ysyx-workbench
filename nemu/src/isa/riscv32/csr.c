#include <common.h>
#include <isa-def.h>
#include "local-include/reg.h"
#include <isa.h>
#include <cpu/cpu.h>

static word_t mvenderid = 0x79737978;
static word_t marchid = 23060238;
static word_t temp;

word_t read_time();

word_t read_timeh();

void difftest_skip_ref();

word_t *get_csr(word_t csr_addr) {  
  temp = 0;
  switch (csr_addr) {
  case 0x140:
    return &cpu.sscratch;
    break;
  case 0x180:
    return &cpu.satp;
    break;
  case 0x300: // mstatus
    return &cpu.mstatus.val;
    break;
  case 0x305: // mtvec
    return &cpu.mtvec;
    break;
  case 0x340: // mscratch
    return &cpu.mscratch;
    break;
  case 0x341: // mepc
    return &cpu.mepc;
    break;
  case 0x342: // mcause
    return &cpu.mcause;
    break;
  case 0xf11: // mvendorid
    // difftest_skip_ref();
    return &mvenderid;
    break;  
  case 0xf12: // marchid
    // difftest_skip_ref();
    return &marchid;
    break;
  case 0x304: // mie
    return &cpu.mie.val;
    break;
  case 0x344: // mip
    return &cpu.mip.val;
    break;
  case 0xC01:
    temp = read_time();
    printf("-------------------------------------rdtime: %u\n",temp);
    return &temp;
    break;
  case 0xC81:
    temp = read_timeh();
    return &temp;
    break;
  case 0x343: // mtval
    return &cpu.mtval;    
  default:
    printf("Invalid csr address: 0x%x\n", csr_addr);
    break;
  }
  return &temp;
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

word_t csrrc(word_t csr_addr, uint32_t value) {
  word_t *csr = get_csr(csr_addr);
  word_t retval = *csr;
  *csr &= ~value;
  return retval;
}

