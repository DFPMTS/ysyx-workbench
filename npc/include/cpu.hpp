#ifndef CPU_HPP
#define CPU_HPP

#include "Vtop.h"
#include <Vtop___024root.h>
#include <iostream>
#include <verilated.h>
#include <verilated_vcd_c.h>

extern Vtop *top;
extern VerilatedVcdC *vcd;

#define concat_temp(x, y) (x##y)
#define REG(x) (concat_temp(top->rootp->top__DOT__regfile__DOT__regs_, x))
#define PC (top->rootp->top__DOT__pc)
#define INST (top->rootp->top__DOT___ifu_inst)
#define IF_RETURN(x)                                                           \
  do {                                                                         \
    if (id == x)                                                               \
      return REG(x);                                                           \
  } while (0);

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