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
#include <memory/vaddr.h>
#include <memory/paddr.h>

// create a mask of length LEN
#define MASK_LEN(LEN) ((1 << (LEN)) - 1)

enum {
  PTE_V,
  PTE_R,
  PTE_W,
  PTE_X
};

// get the bit N of PTE
#define BIT(PTE, N) (((PTE) >> (N)) & 1)

paddr_t isa_mmu_translate(vaddr_t vaddr, int len, int type) {  
  //          31:22  21:12     11:0
  // vaddr = {VPN[1],VPN[0],page_offset}
  // printf("vaddr: 0x%x\n",vaddr);
  word_t VPN_1 = BITS(vaddr, 31, 22);
  word_t VPN_0 = BITS(vaddr, 21, 12);
  word_t page_offset = BITS(vaddr, 11, 0);

  // first level page table addr
  // the PPN is actually 22 bits, but we only take low 20 bit for now
  word_t flpt = BITS(cpu.satp, 19, 0) << PAGE_SHIFT;

  // use VPN[1] to address in to flpt to get slpt
  word_t flpte = paddr_read(flpt | (VPN_1 << 2), 4);
  Assert(BIT(flpte, PTE_V), "Invalid First Level PTE for vaddr: 0x%x\n", vaddr);
  word_t slpt = BITS(flpte, 29, 10) << PAGE_SHIFT;

  // use VPN[0] to address in to slpt to get PTE
  word_t PTE = paddr_read(slpt | (VPN_0 << 2), 4);
  // printf("NEMU: vaddr: 0x%x pdir: 0x%x flpte_p: 0x%x flpte: 0x%x slpte_p: 0x%x "
  //        "slpte: 0x%x\n",
  //        vaddr, flpt, (flpt | (VPN_1 << 2)), flpte, (slpt | (VPN_0 << 2)),
  //        PTE);
  
  // again, only take low 20 bits of PPN
  word_t PPN = BITS(PTE, 29, 10);
  word_t paddr = (PPN << PAGE_SHIFT) | page_offset;

  // check protection bits
  bool V = BIT(PTE, PTE_V);
  bool R = BIT(PTE, PTE_R);
  bool W = BIT(PTE, PTE_W);
  bool X = BIT(PTE, PTE_X);

  Assert(V, "vaddr: 0x%x -> paddr: 0x%x is not Valid", vaddr, paddr);

  switch (type) {
  case MEM_TYPE_READ:
    Assert(R, "vaddr: 0x%x -> paddr: 0x%x is not Readable", vaddr, paddr);
    break;
  case MEM_TYPE_WRITE:
    Assert(W, "vaddr: 0x%x -> paddr: 0x%x is not Writeable", vaddr, paddr);
    break;
  case MEM_TYPE_IFETCH:
    Assert(X, "vaddr: 0x%x -> paddr: 0x%x is not eXecutable", vaddr, paddr);
    break;

  default:
    break;
  }

  return paddr;
}
