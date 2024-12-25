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

  InstInfo inst[128];

  void bindUops() {
    // * renameUop
#define UOP renameUop
#define V_UOP V_RENAME_UOP
#define V_UOP_VALID V_RENAME_VALID
#define V_UOP_READY V_RENAME_READY
#define UOP_FIELDS RENAME_FIELDS
    REPEAT_1(BIND_FIELDS)
    REPEAT_1(BIND_VALID)
    REPEAT_1(BIND_READY)

    // * readRegUop
#define UOP readRegUop
#define V_UOP V_READREG_UOP
#define V_UOP_VALID V_READREG_VALID
#define V_UOP_READY V_READREG_READY
#define UOP_FIELDS READREG_FIELDS

    REPEAT_4(BIND_FIELDS)

    // * writebackUop
#define UOP writebackUop
#define V_UOP V_WRITEBACK_UOP
#define V_UOP_VALID V_WRITEBACK_VALID
#define UOP_FIELDS WRITEBACK_FIELDS

    REPEAT_4(BIND_FIELDS)
    REPEAT_4(BIND_VALID)

    // * commitUop
#define UOP commitUop
#define V_UOP V_COMMIT_UOP
#define V_UOP_VALID V_COMMIT_VALID
#define UOP_FIELDS COMMIT_FIELDS

    REPEAT_1(BIND_FIELDS)
    REPEAT_1(BIND_VALID)
  }

  void log() {
    for (int i = 0; i < 1; ++i) {
      if (*renameUop[i].valid) {
        printf("renameUop[%d]:\n", i);
        printf("  opcode: %d\n", *renameUop[i].opcode);
        printf("  fuType: %d\n", *renameUop[i].fuType);
        printf("  robPtr_index: %d\n", *renameUop[i].robPtr_index);
        printf("  robPtr_flag: %d\n", *renameUop[i].robPtr_flag);
        printf("  src1Ready: %d\n", *renameUop[i].src1Ready);
        printf("  src2Ready: %d\n", *renameUop[i].src2Ready);
        printf("  src1Type: %d\n", *renameUop[i].src1Type);
        printf("  src2Type: %d\n", *renameUop[i].src2Type);
      }
    }
  }
};

#endif