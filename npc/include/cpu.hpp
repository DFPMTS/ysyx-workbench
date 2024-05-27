#ifndef CPU_HPP
#define CPU_HPP

#include "Vtop.h"
#include <Vtop___024root.h>
#include <iostream>
#include <verilated.h>
#include <verilated_vcd_c.h>

extern Vtop *top;
extern VerilatedVcdC *vcd;

#define SIM_T 10
extern uint64_t eval_time;

#define concat_temp(x, y) (x##y)
#define REG(x) (concat_temp(top->rootp->top__DOT__regfile__DOT__regs_, x))
#define PC (top->rootp->top__DOT__pc)
#define DNPC (top->rootp->top__DOT___wb_pc_io_dnpc)
#define RD (top->io_rd)
#define RS1 (top->io_rs1)
#define RS2 (top->io_rs2)
#define JAL (top->io_jal)
#define JALR (top->io_jalr)
#define IMM (top->io_imm)
#define INST (top->io_inst)
#define VALID (top->io_valid)
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