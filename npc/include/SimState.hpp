#ifndef SIMSTATE_HPP
#define SIMSTATE_HPP

#include "Uop.hpp"
#include "cpu.hpp"

class SimState {
public:
  RenameUop renameUop[4];
  WritebackUop writebackUop[4];
  ReadRegUop readRegUop[4];
  CommitUop commitUop[4];

  void bindUops() {
    // * writebackUop
#define UOP writebackUop
#define V_UOP V_WRITEBACK_UOP
#define UOP_FIELDS WRITEBACK_FIELDS

    REPEAT_4(BIND_FIELDS)

    // * readRegUop
#define UOP readRegUop
#define V_UOP V_READREG_UOP
#define UOP_FIELDS READREG_FIELDS

    REPEAT_4(BIND_FIELDS)
  }
};

#endif