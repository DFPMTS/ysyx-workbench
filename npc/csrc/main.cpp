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
  trace_and_difftest();
  while (running) {
    cpu_step();

    ref_difftest_exec(1);
    trace_and_difftest();
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