#include "cpu.hpp"
#include "difftest.hpp"
#include "disasm.hpp"

Vtop *top;
VerilatedVcdC *vcd;
uint64_t eval_time;

static const char *regs[] = {
    "$0", "ra", "sp", "gp", "tp", "t0", "t1", "t2",
    "s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5",
};

static uint32_t *gprs[16];

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

void init_regs() {
#define MAP_REG(i) gprs[i] = &REG(i);
  MAP_REG(0);
  MAP_REG(1);
  MAP_REG(2);
  MAP_REG(3);

  MAP_REG(4);
  MAP_REG(5);
  MAP_REG(6);
  MAP_REG(7);

  MAP_REG(8);
  MAP_REG(9);
  MAP_REG(10);
  MAP_REG(11);

  MAP_REG(12);
  MAP_REG(13);
  MAP_REG(14);
  MAP_REG(15);
}

uint32_t gpr(int id) {
  check_gpr_bound(id);
  return *gprs[id];
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
#ifdef WAVE
  eval_time = sim_time * SIM_T - 2;
  vcd->dump(eval_time);
#endif

  top->clock = 1;
  top->eval();
#ifdef WAVE
  eval_time = sim_time * SIM_T;
  vcd->dump(eval_time);
#endif

  top->clock = 0;
  top->eval();
#ifdef WAVE
  eval_time = sim_time * SIM_T + 2;
  vcd->dump(eval_time);
#endif

#ifdef WAVE
  vcd->flush();
#endif
  ++sim_time;
}

void nvboard_bind_all_pins(Vtop *top);

void init_cpu() {
  top = new Vtop;
#ifdef NVBOARD
  nvboard_bind_all_pins(top);
#endif
  init_regs();

#ifdef WAVE
  vcd = new VerilatedVcdC;
  Verilated::traceEverOn(true);
  top->trace(vcd, 5);
  vcd->open("wave.vcd");
#endif

  int T = 10;
  top->reset = 1;
  while (T--) {
    cpu_step();
  }
  running = 1;
  top->reset = 0;
  top->eval();
}