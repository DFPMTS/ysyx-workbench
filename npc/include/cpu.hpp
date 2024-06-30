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

#ifdef NPC
#define REG(x)                                                                 \
  (concat_temp(top->rootp->npc_top__DOT__npc__DOT__regfile__DOT__regs_, x))
#define PC                                                                     \
  (top->rootp->npc_top__DOT__npc__DOT__pc) // the cycle when VALID is true
#define INST                                                                   \
  (top->rootp->npc_top__DOT__npc__DOT__commit_inst) // one cycle after VALID
#define VALID (top->rootp->npc_top__DOT__npc__DOT___wbu_io_out_valid)
#else
#define REG(x)                                                                    \
  (concat_temp(                                                                   \
      top->rootp                                                                  \
          ->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__regfile__DOT__regs_, \
      x))
#define PC (top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__pc)
#define INST                                                                   \
  (top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__commit_inst)
#define VALID                                                                  \
  (top->rootp                                                                  \
       ->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___wbu_io_out_valid)
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