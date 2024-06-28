#include "debug.hpp"
#include "difftest.hpp"
#include "mem.hpp"
#include "monitor.hpp"
#include "status.hpp"

int main(int argc, char *argv[]) {
  init_monitor(argc, argv);

  int T = 1000;
  bool commit = false;
  uint32_t commit_pc = 0;
  while (running) {
    cpu_step();
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

  return retval;
}