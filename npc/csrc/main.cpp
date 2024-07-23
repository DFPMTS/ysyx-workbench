#include "debug.hpp"
#include "difftest.hpp"
#include "mem.hpp"
#include "monitor.hpp"
#include "status.hpp"

void nvboard_update();
void nvboard_quit();
void nvboard_init(int x);

int main(int argc, char *argv[]) {
  Verilated::commandArgs(argc, argv);
  init_monitor(argc, argv);
  int T = 1000;
  bool commit = false;
  uint32_t commit_pc = 0;
#ifdef NVBOARD
  nvboard_init(1);
#endif
  Log("Simulation begin");
  while (running) {
    cpu_step();
#ifdef NVBOARD
    nvboard_update();
#endif
    if (commit) {
      // check the comments of PC / INST
      trace(commit_pc, INST);
#ifdef DIFFTEST
      difftest();
#endif
      commit = false;
    }
    if (VALID) {
      commit = true;
      commit_pc = PC;
    }
  }
  // a0
  int retval = gpr(10);

#ifdef WAVE
  vcd->close();
#endif

  if (retval == 0) {
    Log("Hit GOOD trap.\n");
  } else {
    Log("Hit BAD trap.\n");
  }
#ifdef NVBOARD
  nvboard_quit();
#endif
  return retval;
}