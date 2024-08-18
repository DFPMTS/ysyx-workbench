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

#include <isa.h>

word_t isa_raise_intr(word_t NO, vaddr_t epc) {
  /* TODO: Trigger an interrupt/exception with ``NO''.
   * Then return the address of the interrupt/exception vector.
   */  
  assert(NO);  
  
  // always trap to M for now
  cpu.mstatus.MPIE = cpu.mstatus.MIE;
  cpu.mstatus.MIE = 0;

  cpu.mstatus.MPP = cpu.priv;

  cpu.mepc = epc;
  cpu.mcause = NO;  
  cpu.priv = PRIV_M;
  // clear trap
  cpu.trap = INTR_EMPTY;

  return cpu.mtvec;
}

uint64_t read_mtime();
uint64_t read_mtimecmp();

word_t isa_query_intr() {

  // set mip/mie
  if (read_mtime() >= read_mtimecmp()) {
    cpu.mip.MTI = 1;
  } else {
    cpu.mip.MTI = 0;
  }
  
  if(cpu.mstatus.MIE){
    if(((Mipe)(cpu.mie.val & cpu.mip.val)).MTI) {
      // machine timer interrupt
      return INTR_MTI;
    }
  }

  return INTR_EMPTY;
}

word_t isa_mret() {
  cpu.priv = cpu.mstatus.MPP;
  cpu.mstatus.MIE = cpu.mstatus.MPIE;
  cpu.mstatus.MPIE = 1;
  cpu.mstatus.MPP = PRIV_U;
  return cpu.mepc;
}

void isa_set_trap(word_t NO, word_t xtval) {
  cpu.trap = NO;
  cpu.mtval = xtval;
}