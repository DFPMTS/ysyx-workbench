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

#ifndef __ISA_RISCV_H__
#define __ISA_RISCV_H__

#include <common.h>

enum {
  PRIV_U,
  PRIV_S,
  PRIV_H,
  PRIV_M
};

typedef union {
  struct {
    uint32_t _WPRI_0 : 1;
    uint32_t SIE : 1;
    uint32_t _WPRI_1 : 1;
    uint32_t MIE : 1;
    uint32_t _WPRI_2 : 1;
    uint32_t SPIE : 1;
    uint32_t UBE : 1;
    uint32_t MPIE : 1;
    uint32_t SPP : 1;
    uint32_t VS : 2;
    uint32_t MPP : 2;
    uint32_t FS : 2;
    uint32_t XS : 2;
    uint32_t MPRV : 1;
    uint32_t SUM : 1;
    uint32_t MXR : 1;
    uint32_t TVM : 1;
    uint32_t TW : 1;
    uint32_t TSR : 1;
    uint32_t _WPRI_3 : 8;
    uint32_t SD : 1;
  };
  uint32_t val;
} Mstatus;

typedef union {
  struct {
    uint32_t _ZERO_0 : 5;
    uint32_t STI : 1;
    uint32_t _ZERO_1 : 1;
    uint32_t MTI : 1;
    uint32_t _ZERO_2 : 24;
  };
  uint32_t val;
} Mipe;

typedef struct {
  word_t gpr[MUXDEF(CONFIG_RVE, 16, 32)];
  vaddr_t pc;
  Mstatus mstatus;
  word_t mcause;
  word_t mepc;
  word_t priv;
  
  word_t mtval;
  word_t mscratch;
  word_t mtvec;
  word_t satp;  
  word_t sscratch;    
  Mipe mip;
  Mipe mie;

  // helper fields  
  word_t trap;
  word_t reservation;
} MUXDEF(CONFIG_RV64, riscv64_CPU_state, riscv32_CPU_state);

word_t csrrw(word_t csr, uint32_t value);
word_t csrrs(word_t csr, uint32_t value);
word_t csrrc(word_t csr, uint32_t value);

// decode
typedef struct {
  union {
    uint32_t val;
  } inst;
} MUXDEF(CONFIG_RV64, riscv64_ISADecodeInfo, riscv32_ISADecodeInfo);

#define isa_mmu_check(vaddr, len, type) ((cpu.satp >> 31) & 1)

#endif
