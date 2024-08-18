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

#include "local-include/reg.h"
#include <cpu/cpu.h>
#include <cpu/ifetch.h>
#include <cpu/decode.h>

#define regW(i) spec_rd = i, spec_rd_val
#define regR(i) gpr(i)
#define Mr vaddr_read
#define Mw vaddr_write

void itrace_generate(Decode *s);
void ftrace_log(Decode *s, int rd, int rs1, word_t offset);

void cycle_mtime();

static word_t min(word_t a, word_t b, bool is_unsigned)
{
  if(is_unsigned) {
    return a < b ? a : b;
  }else{
    return (int32_t)a < (int32_t)b ? a : b;
  }
}

static word_t max(word_t a, word_t b, bool is_unsigned)
{
  if(is_unsigned) {
    return a > b ? a : b;
  }else{
    return (int32_t)a > (int32_t)b ? a : b;
  }
}

enum {
  TYPE_R, TYPE_R_SHIFT, TYPE_I, TYPE_I_SHIFT, TYPE_U, TYPE_S, TYPE_J, TYPE_B, 
  TYPE_N, // none
};

#define src1R() do { *src1 = regR(rs1); } while (0)
#define src2R() do { *src2 = regR(rs2); } while (0)
#define src2R_SHIFT() do { *src2 = BITS(regR(rs2), 4, 0); } while (0)
#define immI() do { *imm = SEXT(BITS(i, 31, 20), 12); } while(0)
#define immI_SHIFT() do { *imm = BITS(i, 24, 20); } while(0)
#define immU() do { *imm = SEXT(BITS(i, 31, 12), 20) << 12; } while(0)
#define immS() do { *imm = (SEXT(BITS(i, 31, 25), 7) << 5) | BITS(i, 11, 7); } while(0)
#define immB() do { *imm = (SEXT(BITS(i, 31, 31), 1) << 12)\
                          | (BITS(i, 7, 7) << 11)\
                          | (BITS(i, 30, 25) << 5)\
                          | (BITS(i, 11, 8) << 1); } while(0)
#define immJ() do { *imm = (SEXT(BITS(i, 31, 31), 1) << 20)\
                          | (BITS(i, 19, 12) << 12)\
                          | (BITS(i, 20, 20) << 11)\
                          | (BITS(i, 30, 21) << 1); } while(0)

static void decode_operand(Decode *s, int *rd, word_t *src1, word_t *src2, word_t *imm, int type) {
  uint32_t i = s->isa.inst.val;
  int rs1 = BITS(i, 19, 15);
  int rs2 = BITS(i, 24, 20);
  *rd     = BITS(i, 11, 7);
  switch (type) {
    case TYPE_R: src1R(); src2R();         break;
    case TYPE_R_SHIFT: src1R(); src2R_SHIFT();   break;
    case TYPE_I: src1R();          immI(); break;
    case TYPE_I_SHIFT: src1R();    immI_SHIFT(); break;
    case TYPE_U:                   immU(); break;
    case TYPE_S: src1R(); src2R(); immS(); break;
    case TYPE_B: src1R(); src2R(); immB(); break;
    case TYPE_J:                   immJ(); break;             
  }
}

void difftest_skip_ref();

uint64_t read_mtime();
uint64_t read_mtimecmp();

static int decode_exec(Decode *s) {
  uint32_t i = s->isa.inst.val;
  int rd  = 0;
  int rs1 = BITS(i, 19, 15);
  word_t src1 = 0, src2 = 0, imm = 0;
  s->dnpc = s->snpc;

  // will only write to cpu state when there's no exception/interrupt
  word_t spec_rd = 0, spec_rd_val = 0;
  word_t mr_val = 0;

#define INSTPAT_INST(s) ((s)->isa.inst.val)
#define INSTPAT_MATCH(s, name, type, ... /* execute body */ ) { \
  decode_operand(s, &rd, &src1, &src2, &imm, concat(TYPE_, type)); \
  __VA_ARGS__ ; \
}

  cycle_mtime();


  // interrupt
  word_t intr;
  if ((intr = isa_query_intr()) != INTR_EMPTY) {
    // Log("interrupt mtime: %lu mtimecmp: %lu pc: 0x%x mtvec: %x", read_mtime(),read_mtimecmp(),cpu.pc,cpu.mtvec);    
    s->dnpc = isa_raise_intr(intr, cpu.pc);
    // difftest_skip_ref();    
    return 0;
  }  

  INSTPAT_START();
  // -----------------------------------------------RV32I-----------------------------------------------
  INSTPAT("??????? ????? ????? ??? ????? 01101 11", lui    , U, regW(rd) = imm);  
  INSTPAT("??????? ????? ????? ??? ????? 00101 11", auipc  , U, regW(rd) = s->pc + imm);

  INSTPAT("??????? ????? ????? ??? ????? 11011 11", jal    , J, regW(rd) = s->snpc, s->dnpc = s->pc + imm, ftrace_log(s, rd, -1, imm));
  INSTPAT("??????? ????? ????? ??? ????? 11001 11", jalr   , I, regW(rd) = s->snpc, s->dnpc = (src1 + imm) & (~1), ftrace_log(s, rd, rs1, imm));

  INSTPAT("??????? ????? ????? 000 ????? 11000 11", beq    , B, s->dnpc = (src1 == src2) ? (s->pc + imm) : s->snpc);
  INSTPAT("??????? ????? ????? 001 ????? 11000 11", bne    , B, s->dnpc = (src1 != src2) ? (s->pc + imm) : s->snpc);
  INSTPAT("??????? ????? ????? 100 ????? 11000 11", blt    , B, s->dnpc = ((int32_t)src1 < (int32_t)src2) ? (s->pc + imm) : s->snpc);
  INSTPAT("??????? ????? ????? 101 ????? 11000 11", bge    , B, s->dnpc = ((int32_t)src1 >= (int32_t)src2) ? (s->pc + imm) : s->snpc);
  INSTPAT("??????? ????? ????? 110 ????? 11000 11", bltu   , B, s->dnpc = (src1 < src2) ? (s->pc + imm) : s->snpc);
  INSTPAT("??????? ????? ????? 111 ????? 11000 11", bgeu   , B, s->dnpc = (src1 >= src2) ? (s->pc + imm) : s->snpc);

  INSTPAT("??????? ????? ????? 000 ????? 00000 11", lb     , I, regW(rd) = SEXT(Mr(src1 + imm, 1), 8));  
  INSTPAT("??????? ????? ????? 001 ????? 00000 11", lh     , I, regW(rd) = SEXT(Mr(src1 + imm, 2), 16));
  INSTPAT("??????? ????? ????? 010 ????? 00000 11", lw     , I, regW(rd) = Mr(src1 + imm, 4));
  INSTPAT("??????? ????? ????? 100 ????? 00000 11", lbu    , I, regW(rd) = Mr(src1 + imm, 1));
  INSTPAT("??????? ????? ????? 101 ????? 00000 11", lhu    , I, regW(rd) = Mr(src1 + imm, 2));
  
  INSTPAT("??????? ????? ????? 000 ????? 01000 11", sb     , S, Mw(src1 + imm, 1, src2));
  INSTPAT("??????? ????? ????? 001 ????? 01000 11", sh     , S, Mw(src1 + imm, 2, src2));
  INSTPAT("??????? ????? ????? 010 ????? 01000 11", sw     , S, Mw(src1 + imm, 4, src2));

  INSTPAT("??????? ????? ????? 000 ????? 00100 11", addi   , I, regW(rd) = src1 + imm);
  INSTPAT("??????? ????? ????? 010 ????? 00100 11", slti   , I, regW(rd) = (int32_t)src1 < (int32_t)imm);
  INSTPAT("??????? ????? ????? 011 ????? 00100 11", sltiu  , I, regW(rd) = src1 < imm);  
  INSTPAT("??????? ????? ????? 100 ????? 00100 11", xori   , I, regW(rd) = src1 ^ imm);
  INSTPAT("??????? ????? ????? 110 ????? 00100 11", ori    , I, regW(rd) = src1 | imm);
  INSTPAT("??????? ????? ????? 111 ????? 00100 11", andi   , I, regW(rd) = src1 & imm);
  INSTPAT("0000000 ????? ????? 001 ????? 00100 11", slli   , I_SHIFT, regW(rd) = src1 << imm);  
  INSTPAT("0000000 ????? ????? 101 ????? 00100 11", srli   , I_SHIFT, regW(rd) = src1 >> imm);
  INSTPAT("0100000 ????? ????? 101 ????? 00100 11", srai   , I_SHIFT, regW(rd) = (int32_t)src1 >> imm);  
  
  INSTPAT("0000000 ????? ????? 000 ????? 01100 11", add    , R, regW(rd) = src1 + src2);
  INSTPAT("0100000 ????? ????? 000 ????? 01100 11", sub    , R, regW(rd) = src1 - src2);
  INSTPAT("0000000 ????? ????? 001 ????? 01100 11", sll    , R_SHIFT, regW(rd) = src1 << src2);
  INSTPAT("0000000 ????? ????? 010 ????? 01100 11", slt    , R, regW(rd) = (int32_t)src1 < (int32_t)src2);
  INSTPAT("0000000 ????? ????? 011 ????? 01100 11", sltu   , R, regW(rd) = src1 < src2);
  INSTPAT("0000000 ????? ????? 100 ????? 01100 11", xor    , R, regW(rd) = src1 ^ src2);
  INSTPAT("0000000 ????? ????? 101 ????? 01100 11", srl    , R_SHIFT, regW(rd) = src1 >> src2);
  INSTPAT("0100000 ????? ????? 101 ????? 01100 11", sra    , R_SHIFT, regW(rd) = (int32_t)src1 >> src2);
  INSTPAT("0000000 ????? ????? 110 ????? 01100 11", or     , R, regW(rd) = src1 | src2);
  INSTPAT("0000000 ????? ????? 111 ????? 01100 11", and    , R, regW(rd) = src1 & src2);
  INSTPAT("???? ???? ???? ????? 000 ????? 00011 11", fence , R, ); // nop
  INSTPAT("0000000 00000 00000 000 00000 11100 11", ecall  , N, isa_set_trap(8 + cpu.priv, 0)); // ecall from M/S/U-mode
  INSTPAT("0000000 00001 00000 000 00000 11100 11", ebreak , N, NEMUTRAP(s->pc, regR(10))); // R(10) is $a0

  // -----------------------------------------------RV32M-----------------------------------------------
  INSTPAT("0000001 ????? ????? 000 ????? 01100 11", mul    , R, regW(rd) = src1 * src2);
  INSTPAT("0000001 ????? ????? 001 ????? 01100 11", mulh   , R, regW(rd) = (SEXT(src1, 32) * SEXT(src2, 32)) >> 32);
  INSTPAT("0000001 ????? ????? 010 ????? 01100 11", mulhsu , R, regW(rd) = (SEXT(src1, 32) * (uint64_t)src2) >> 32);
  INSTPAT("0000001 ????? ????? 011 ????? 01100 11", mulhu  , R, regW(rd) = ((uint64_t)src1 * (uint64_t)src2) >> 32);
  INSTPAT("0000001 ????? ????? 100 ????? 01100 11", div    , R, regW(rd) = src2 == 0 ? -1 : (int32_t)src1 / (int32_t)src2);
  INSTPAT("0000001 ????? ????? 101 ????? 01100 11", divu   , R, regW(rd) = src2 == 0 ? -1 : src1 / src2);
  INSTPAT("0000001 ????? ????? 110 ????? 01100 11", rem    , R, regW(rd) = (int32_t)src1 % (int32_t)src2);
  INSTPAT("0000001 ????? ????? 111 ????? 01100 11", remu   , R, regW(rd) = src1 % src2);

  // -----------------------------------------------Zicsr-----------------------------------------------  
  // ! Note that there are special rules for rd=x0/rs1=x0, but we omitted them here for simplicity
  // ! Also, we assume all CSR bits are writable
  INSTPAT("??????? ????? ????? 001 ????? 11100 11", csrrw  , I, regW(rd) = csrrw(BITS(imm,11,0), src1));
  INSTPAT("??????? ????? ????? 010 ????? 11100 11", csrrs  , I, regW(rd) = csrrs(BITS(imm,11,0), src1));
  INSTPAT("??????? ????? ????? 011 ????? 11100 11", csrrc  , I, regW(rd) = csrrc(BITS(imm,11,0), src1));
  INSTPAT("??????? ????? ????? 101 ????? 11100 11", csrrwi , I, regW(rd) = csrrw(BITS(imm,11,0), BITS(i, 19, 15)));
  INSTPAT("??????? ????? ????? 110 ????? 11100 11", csrrsi , I, regW(rd) = csrrs(BITS(imm,11,0), BITS(i, 19, 15)));
  INSTPAT("??????? ????? ????? 111 ????? 11100 11", csrrci , I, regW(rd) = csrrc(BITS(imm,11,0), BITS(i, 19, 15)));

  //----------------------------------------------Zifencei-----------------------------------------------
  INSTPAT("??????? ????? ????? 001 ????? 00011 11", fence.i, I, ); // just nop

  // --------------------------------------------Trap-Return--------------------------------------------
  INSTPAT("0011000 00010 00000 000 00000 11100 11", mret   , N, s->dnpc = isa_mret());    

  // ---------------------------------------Interrupt-Management----------------------------------------
  INSTPAT("0001000 00101 00000 000 00000 11100 11", wfi    , N, /*difftest_skip_ref()*/);     // nop

  // -----------------------------------------------RV32A-----------------------------------------------
  INSTPAT("00010 ? ? 00000 ????? 010 ????? 0101111", lr.w  ,      R, /*printf("LR 0x%x\n",src1),*/ cpu.reservation = src1, regW(rd) = Mr(src1, 4));
  INSTPAT("00011 ? ? ????? ????? 010 ????? 0101111", sc.w  ,      R, do {if(cpu.reservation == src1) {regW(rd) = 0; Mw(src1, 4, src2); /*printf("SC: succ %x\n",src1);*/ }else{regW(rd) = 1;/*printf("SC: fail %x\n",src1);*/}  cpu.reservation = 0; }while(0) );
  INSTPAT("00001 ? ? ????? ????? 010 ????? 0101111", amoswap.w  , R, regW(rd) = (mr_val = Mr(src1, 4)), Mw(src1, 4, src2));
  INSTPAT("00000 ? ? ????? ????? 010 ????? 0101111", amoadd.w   , R, regW(rd) = (mr_val = Mr(src1, 4)), Mw(src1, 4, mr_val + src2));
  INSTPAT("00100 ? ? ????? ????? 010 ????? 0101111", amoxor.w   , R, regW(rd) = (mr_val = Mr(src1, 4)), Mw(src1, 4, mr_val ^ src2));
  INSTPAT("01100 ? ? ????? ????? 010 ????? 0101111", amoand.w   , R, regW(rd) = (mr_val = Mr(src1, 4)), Mw(src1, 4, mr_val & src2));
  INSTPAT("01000 ? ? ????? ????? 010 ????? 0101111", amoor.w    , R, regW(rd) = (mr_val = Mr(src1, 4)), Mw(src1, 4, mr_val | src2));
  INSTPAT("10000 ? ? ????? ????? 010 ????? 0101111", amomin.w   , R, regW(rd) = (mr_val = Mr(src1, 4)), Mw(src1, 4, min(mr_val, src2, false)));
  INSTPAT("10100 ? ? ????? ????? 010 ????? 0101111", amomax.w   , R, regW(rd) = (mr_val = Mr(src1, 4)), Mw(src1, 4, max(mr_val, src2, false)));
  INSTPAT("11000 ? ? ????? ????? 010 ????? 0101111", amominu.w  , R, regW(rd) = (mr_val = Mr(src1, 4)), Mw(src1, 4, min(mr_val, src2, true)));
  INSTPAT("11100 ? ? ????? ????? 010 ????? 0101111", amomaxu.w  , R, regW(rd) = (mr_val = Mr(src1, 4)), Mw(src1, 4, max(mr_val, src2, true)));

  // INVALID  
  INSTPAT("??????? ????? ????? ??? ????? ????? ??", inv    , N, INV(s->pc));
  INSTPAT_END();
  
  // exception
  if (HAS_TRAP) {
    // Log("Trap");
    s->dnpc = isa_raise_intr(cpu.trap, cpu.pc);
    return 0;
  }

  // inst commit
  if (spec_rd) {
    gpr(spec_rd) = spec_rd_val;
  }

  return 0;
}

int isa_exec_once(Decode *s) {
  cpu.trap = INTR_EMPTY;
  s->isa.inst.val = inst_fetch(&s->snpc, 4);
  if(HAS_TRAP) {
    isa_raise_intr(cpu.trap, cpu.pc);
    return 0;
  }
  itrace_generate(s);
  return decode_exec(s);
}
