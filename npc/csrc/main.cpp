#include "EventMonitor.hpp"
#include "debug.hpp"
#include "difftest.hpp"
#include "mem.hpp"
#include "monitor.hpp"
#include "status.hpp"
#include <chrono>
#include <cstdint>

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
class SimulationSpeed {
private:
  // Record the start time
  std::chrono::time_point<std::chrono::system_clock> start;

public:
  void initTimer() { start = std::chrono::system_clock::now(); }
  void printSimulationSpeed(uint64_t cycles) {
    auto end = std::chrono::system_clock::now();
    std::chrono::duration<double> elapsed_seconds = end - start;
    std::cout << "Simulation Speed:  " << cycles / elapsed_seconds.count()
              << " cycles/s" << std::endl;
  }
};

int main(int argc, char *argv[]) {
  Verilated::commandArgs(argc, argv);
  init_monitor(argc, argv);
  SimulationSpeed sim_speed;
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
  sim_speed.initTimer();
  int runT = 100;
  while (runT--) {
    cpu_step();
    //     ++totalCycles;
    // #ifdef NVBOARD
    //     nvboard_update();
    // #endif
    //     if (commit) {
    //       // check the comments of PC / INST
    // #ifdef DIFFTEST
    //       difftest();
    // #endif
    //       commit = false;
    //     }
    //     if (isCommit()) {
    //       commit = true;
    //       trace(PC, INST);
    //       if (PC == 0xa0000000) {
    //         begin_wave = true;
    //         printPerfCounters();
    //         clearAllEventCount();
    //         totalCycles = 0;
    //         booted = true;
    //       }
    //     }
    //     if (totalCycles % 10000000 == 0) {
    //       std::cerr << "Total cycles: " << totalCycles << std::endl;
    //       printPerfCounters();
    //     }
  }
  std::cerr << "Simulation End" << std::endl;
  ++totalCycles;
  // a0
  int retval = 0;

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
  sim_speed.printSimulationSpeed(totalCycles);
  return retval;
}