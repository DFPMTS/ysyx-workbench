#include "testRename_tb.h"
#include "verilated.h"
#include "verilated_fst_c.h"
#include <cstdint>
#include <iostream>
#include <queue>
#include <vector>

#define WAVE
#define SIM_T 10
bool begin_wave = true;
VerilatedFstC *fst = nullptr;
uint64_t eval_time;

// * testing the FreeList module
testRename_tb *top = nullptr;

void step() {
  static uint64_t sim_time = 1;
  top->clock = 0;

  top->eval();
#ifdef WAVE
  if (begin_wave) {
    eval_time = sim_time * SIM_T - 2;
    fst->dump(eval_time);
  }
#endif

  top->clock = 1;
  top->eval();
#ifdef WAVE
  if (begin_wave) {
    eval_time = sim_time * SIM_T;
    fst->dump(eval_time);
  }
#endif

  top->clock = 0;
  top->eval();
#ifdef WAVE
  if (begin_wave) {
    eval_time = sim_time * SIM_T + 2;
    fst->dump(eval_time);
  }
#endif

#ifdef WAVE
  if (begin_wave) {
    fst->flush();
  }
#endif
  ++sim_time;
}

void reset() {
  top->reset = 1;
  step();
  top->reset = 0;
  step();
}

int main() {
  top = new testRename_tb;
  fst = new VerilatedFstC;
  Verilated::traceEverOn(true);
  top->trace(fst, 5);
  fst->open("testRename.fst");

  int T = 0;
  reset();
  std::queue<uint32_t> pregs;
  uint32_t x10_map = 0;

  std::vector<uint32_t> insts = {0x002182B3, 0x00220333, 0x00138233,
                                 0xFFF08313};

  while (T < 300) {
    top->io_inst = insts[T % insts.size()];
    top->io_valid = 1;

    step();
    if (top->io_renameUop_0_valid) {
      std::cout << "[rd]: " << (uint32_t)top->io_renameUop_0_bits_prd << " -- ";
      std::cout << "[rs1]: " << (uint32_t)top->io_renameUop_0_bits_prs1
                << " -- ";
      std::cout << "[rs2]: " << (uint32_t)top->io_renameUop_0_bits_prs2
                << std::endl;
    }

    ++T;
  }
  fst->close();
  delete top;
  return 0;
}