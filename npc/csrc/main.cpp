#include "debug.hpp"
#include "difftest.hpp"
#include "disasm.hpp"
#include "mem.hpp"
#include "monitor.hpp"
#include "status.hpp"

int main(int argc, char *argv[]) {
  init_monitor(argc, argv);
  // char *img = argc == 2 ? argv[1] : NULL;
  // load_img(img);
  init_disasm("riscv32-pc-linux-gnu");
  // init_cpu();
  // init_cpu must happens **before** init_difftest
  // init_difftest();
  // trace_and_difftest();

  int T = 1000;
  bool difftest_check = false;
  while (running) {
    cpu_step();
    if (difftest_check) {
      difftest();
      difftest_check = false;
    }
    if (VALID) {
      // cpu_step();
      trace();
      difftest_check = true;
    }
  }
  // a0
  int retval = gpr(10);

  vcd->close();

  if (retval == 0) {
    Log("Hit GOOD trap.\n");
  } else {
    Log("Hit BAD trap.\n");
  }

  return retval;
}