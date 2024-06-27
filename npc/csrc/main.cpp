#include "debug.hpp"
#include "difftest.hpp"
#include "mem.hpp"
#include "monitor.hpp"
#include "status.hpp"

int main(int argc, char *argv[]) {
  init_monitor(argc, argv);

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