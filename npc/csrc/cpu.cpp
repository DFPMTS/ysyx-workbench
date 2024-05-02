#include "cpu.hpp"
#include "difftest.hpp"
#include "disasm.hpp"

Vtop *top;
VerilatedVcdC *vcd;

static const char *regs[] = {
    "$0", "ra", "sp", "gp", "tp", "t0", "t1", "t2",
    "s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5",
};

void check_gpr_bound(int id) { assert(id >= 0 && id < 16); }

void isa_reg_display(difftest_context_t *ctx) {

  static char buf[128];
  disassemble(buf, 128, PC, (uint8_t *)&INST, 4);

  printf("------------------------\n");
  printf("|PC:    %08X       |\n", ctx->pc);
  printf("------------------------\n");
  printf("|       %s\n", buf);
  printf("------------------------\n");
  for (int i = 0; i < 16; ++i) {
    printf("%s\t%08X\t%d\n", reg_name(i), ctx->gpr[i], ctx->gpr[i]);
  }
  printf("============================================\n");
}

const char *reg_name(int id) {
  check_gpr_bound(id);
  return regs[id];
}

uint32_t gpr(int id) {
  check_gpr_bound(id);
  IF_RETURN(0);
  IF_RETURN(1);
  IF_RETURN(2);
  IF_RETURN(3);

  IF_RETURN(4);
  IF_RETURN(5);
  IF_RETURN(6);
  IF_RETURN(7);

  IF_RETURN(8);
  IF_RETURN(9);
  IF_RETURN(10);
  IF_RETURN(11);

  IF_RETURN(12);
  IF_RETURN(13);
  IF_RETURN(14);
  IF_RETURN(15);

  assert(0);
}

void get_context(difftest_context_t *dut) {
  for (int i = 0; i < 16; ++i) {
    dut->gpr[i] = gpr(i);
  }
  dut->pc = PC;
}

void cpu_step() {
  static uint64_t sim_time = 1;
  top->clock = 0;
  top->eval();
  // vcd->dump(sim_time * 10 - 2);

  top->clock = 1;
  top->eval();
  // vcd->dump(sim_time * 10);

  top->clock = 0;
  top->eval();
  // vcd->dump(sim_time * 10 + 2);

  // vcd->flush();
  ++sim_time;
}

void init_cpu() {
  top = new Vtop;
  vcd = new VerilatedVcdC;
  Verilated::traceEverOn(true);
  top->trace(vcd, 5);
  vcd->open("wave.vcd");

  int T = 3;
  top->reset = 1;
  while (T--) {
    cpu_step();
  }
  running = 1;
  top->reset = 0;
  top->eval();
}