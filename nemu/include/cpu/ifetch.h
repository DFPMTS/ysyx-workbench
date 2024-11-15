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

#ifndef __CPU_IFETCH_H__

#include <memory/vaddr.h>
#include <isa.h>

static inline uint32_t inst_fetch(vaddr_t *pc, int len) {
  // RVC enables an 32-bit inst to cross a page boundry
  // in this situation, split this into two access when paging is on
  // note that sv** page table supports superpage, 
  // but we just consider 4KB pages for convenience

  bool need_split = isa_mmu_check(*pc, len, MEM_TYPE_IFETCH) && ((*pc + 2) & PAGE_MASK) == 0;

  if (need_split) {
    // fetch low 2 bytes
    uint32_t inst = vaddr_ifetch(*pc, 2);
    (*pc) += 2;
    if(HAS_TRAP || isRVC(inst)) {
      // early return if exception or a RVC inst fetched
      return inst;
    }
    // fetch high 2 bytes
    inst = inst | (vaddr_ifetch(*pc, 2) << 16);
    (*pc) += 2;
    return inst;
  } else {
    uint32_t inst = vaddr_ifetch(*pc, len);
    (*pc) += len;
    return inst;
  }
}

#endif
