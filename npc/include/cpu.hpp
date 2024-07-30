#ifndef CPU_HPP
#define CPU_HPP

#include "Vtop.h"
#include "config.hpp"
#include <Vtop___024root.h>
#include <iostream>
#include <verilated.h>
#include <verilated_fst_c.h>

extern Vtop *top;
extern VerilatedFstC *fst;

#define SIM_T 10
extern uint64_t eval_time;
extern bool begin_wave;

#define CONCAT(x, y) concat_temp(x, y)
#define concat_temp(x, y) x##y
static uint32_t dummy = 0;

#define IFU CONCAT(CPU, __DOT__ifu__DOT__)
#define IDU CONCAT(CPU, __DOT__idu__DOT__)
#define REGFILE CONCAT(IDU, regfile__DOT__regs_)
#define EXU CONCAT(CPU, __DOT__exu__DOT__)
#define MEM CONCAT(CPU, __DOT__mem__DOT__)
#define WBU CONCAT(CPU, __DOT__wbu__DOT__)

#define REG(x) (CONCAT(REGFILE, x))
#define PC CONCAT(IFU, pc)
#define INST CONCAT(WBU, ctrlBuffer_inst)
#define VALID CONCAT(WBU, validBuffer)
#define JUMP                                                                   \
  (CONCAT(WBU, ctrlBuffer_fuType) == 1 && CONCAT(WBU, ctrlBuffer_fuOp == 0))
#define RD CONCAT(WBU, ctrlBuffer_rd)
#define RS1 CONCAT(WBU, ctrlBuffer_rs1)
#define IMM CONCAT(WBU, dataBuffer_imm)
#define DNPC                                                                   \
  (CONCAT(WBU, dnpcBuffer_valid) ? CONCAT(WBU, dnpcBuffer_pc) : PC + 4)

#ifdef NPC
#define CPU top->rootp->npc_top__DOT__npc
#else
#define CPU top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu
#endif
struct difftest_context_t {
  uint32_t gpr[16];
  uint32_t pc;
};

uint32_t gpr(int id);

const char *reg_name(int id);
void get_context(difftest_context_t *dut);

void isa_reg_display(difftest_context_t *ctx);
void init_cpu();
void cpu_step();

#endif