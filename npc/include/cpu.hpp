#ifndef CPU_HPP
#define CPU_HPP

#include "Vtop.h"
#include "config.hpp"
#include <Vtop___024root.h>
#include <iostream>
#include <verilated.h>
#include <verilated_vcd_c.h>

extern Vtop *top;
extern VerilatedVcdC *vcd;

#define SIM_T 10
extern uint64_t eval_time;

#define concat_temp(x, y) (x##y)
static uint32_t dummy = 0;
#ifdef NPC
#define REG(x)                                                                 \
  (concat_temp(                                                                \
      top->rootp->npc_top__DOT__npc__DOT__idu__DOT__regfile__DOT__regs_, x))
#define PC (top->rootp->npc_top__DOT__npc__DOT__ifu__DOT__pc)
#define INST (top->rootp->npc_top__DOT__npc__DOT__wbu__DOT__ctrlBuffer_inst)
#define VALID (top->rootp->npc_top__DOT__npc__DOT__wbu__DOT__validBuffer)
#define JUMP                                                                   \
  (top->rootp->npc_top__DOT__npc__DOT__wbu__DOT__ctrlBuffer_fuType == 1 &&     \
   top->rootp->npc_top__DOT__npc__DOT__wbu__DOT__ctrlBuffer_fuOp == 0)
#define RD (top->rootp->npc_top__DOT__npc__DOT__wbu__DOT__ctrlBuffer_rd)
#define RS1 (top->rootp->npc_top__DOT__npc__DOT__wbu__DOT__ctrlBuffer_rs1)
#define IMM (top->rootp->npc_top__DOT__npc__DOT__wbu__DOT__dataBuffer_imm)
#define DNPC                                                                   \
  (top->rootp->npc_top__DOT__npc__DOT__wbu__DOT__dnpcBuffer_valid              \
       ? top->rootp->npc_top__DOT__npc__DOT__wbu__DOT__dnpcBuffer_pc           \
       : PC + 4)

#else
#define REG(x)                                                                              \
  (concat_temp(                                                                             \
      top->rootp                                                                            \
          ->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__idu__DOT__regfile__DOT__regs_, \
      x))
#define PC                                                                     \
  (top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__ifu__DOT__pc)
#define INST                                                                   \
  (top->rootp                                                                  \
       ->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__wbu__DOT__dbgInBuffer_inst)
#define VALID                                                                  \
  (top->rootp                                                                  \
       ->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__wbu__DOT__validBuffer)
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