#include "EventMonitor.hpp"
#include "debug.hpp"
#include "difftest.hpp"
#include "mem.hpp"
#include "monitor.hpp"
#include "status.hpp"

void nvboard_update();
void nvboard_quit();
void nvboard_init(int x);

static uint64_t totalCycles = 0;

void printPerfCounters() {
  std::cout << "-----------------------------------" << std::endl;
  std::cout << "ifuFinished" << " " << getEventCount("ifuFinished")
            << std::endl;
  std::cout << "icacheMiss" << " " << getEventCount("icacheMiss") << std::endl;
  std::cout << "ifuStalled" << " " << getEventCount("ifuStalled") << std::endl;

  std::cout << "iduBruInst" << " " << getEventCount("iduBruInst") << std::endl;
  std::cout << "iduAluInst" << " " << getEventCount("iduAluInst") << std::endl;
  std::cout << "iduMemInst" << " " << getEventCount("iduMemInst") << std::endl;
  std::cout << "iduCsrInst" << " " << getEventCount("iduCsrInst") << std::endl;

  std::cout << "memFinished" << " " << getEventCount("memFinished")
            << std::endl;
  std::cout << "memStalled" << " " << getEventCount("memStalled") << std::endl;

  std::cout << "Total cycles: " << totalCycles << std::endl;

  std::cout << "-----------------------------------" << std::endl;
}

int main(int argc, char *argv[]) {
  Verilated::commandArgs(argc, argv);
  init_monitor(argc, argv);

  bool commit = false;
  bool booted = false;
#ifdef NVBOARD
  nvboard_init(1);
#endif
  Log("Simulation begin");
  begin_wave = true;
  // int T = 1000000;
  // while (true) {
  //   trace_pc();
  // }
  // return 0;
  // begin_wave = true;

  while (running) {
    cpu_step();
    ++totalCycles;
#ifdef NVBOARD
    nvboard_update();
#endif
    if (commit) {
      // check the comments of PC / INST
#ifdef DIFFTEST
      difftest();
#endif
      commit = false;
    }
    if (VALID) {
      commit = true;
      trace(PC, INST);
      if (PC == 0xa0000000) {
        begin_wave = true;
        printPerfCounters();
        clearAllEventCount();
        totalCycles = 0;
        booted = true;
      }
    }
    if (totalCycles % 1000000 == 0) {
      std::cerr << "Total cycles: " << totalCycles << std::endl;
    }
  }
  ++totalCycles;
  // a0
  int retval = gpr(10);

#ifdef WAVE
  fst->close();
#endif

  if (retval == 0) {
    Log("Hit GOOD trap.\n");
  } else {
    Log("Hit BAD trap.\n");
  }
#ifdef NVBOARD
  nvboard_quit();
#endif
  printPerfCounters();
  return retval;
}