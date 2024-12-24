#ifndef STATUS_HPP
#define STATUS_HPP

#include <cstdint>

using CData = uint8_t;
using IData = uint32_t;

enum class SrcType : CData { ZERO = 0, REG = 1, IMM = 2, PC = 3 };

enum class FuType : CData {
  ALU = 0,
  BRU = 1,
  LSU = 2,
  MUL = 3,
  DIV = 4,
  AGU = 5,
  CSR = 6
};

enum class ALUOp : CData {
  ADD = 0b0000,
  SUB = 0b0001,
  LEFT = 0b0010,
  RIGHT = 0b0011,
  AND = 0b0100,
  OR = 0b0101,
  XOR = 0b0110,
  ARITH = 0b0111,
  EQ = 0b1010,
  NE = 0b1011,
  LT = 0b1100,
  LTU = 0b1101,
  GE = 0b1110,
  GEU = 0b1111
};

enum class BRUOp : CData {
  AUIPC = 0b0000,
  JALR = 0b1000,
  JAL = 0b1001,
  BEQ = 0b1010,
  BNE = 0b1011,
  BLT = 0b1100,
  BLTU = 0b1101,
  BGE = 0b1110,
  BGEU = 0b1111
};

enum class LSUOp : CData {
  LB = 0b0000,
  LH = 0b0001,
  LW = 0b0010,
  LBU = 0b0100,
  LHU = 0b0101,
  SB = 0b1000,
  SH = 0b1001,
  SW = 0b1010
};

enum class CSROp : CData {
  CSRR = 0b0000,
  CSRRW = 0b0001,
  CSRRS = 0b0010,
  CSRRC = 0b0011,
  CSRRWI = 0b0100,
  CSRRSI = 0b0101,
  CSRRCI = 0b0110,
  SRET = 0b1000,
  MRET = 0b1001,
  ECALL = 0b1010,
  EBREAK = 0b1011
};

enum class ImmType : CData { I = 0, U = 1, S = 2, B = 3, J = 4, X = 0xFF };

enum class CImmType : CData {
  LWSP = 0,
  ADDI = 1,
  SLLI = 2,
  SLTI = 3,
  ADDI16SP = 4,
  LUI = 5,
  CSS_SW = 6,
  CIW = 7,
  LW = 8,
  CS_SW = 9,
  CA = 10,
  BRANCH = 11,
  SHIFT = 12,
  ANDI = 13,
  CJ = 14,
  X = 0xFF
};

enum class Flags : CData { NOTHING = 0, MISPREDICT = 1 };

struct RenameUop {
  CData *rd;
  CData *prd;
  CData *prs1;
  CData *prs2;
  SrcType *src1Type;
  SrcType *src2Type;
  CData *src1Ready;
  CData *src2Ready;
  CData *robPtr_flag;
  CData *robPtr_index;
  IData *ldqIndex;
  IData *stqIndex;
  IData *imm;
  IData *pc;
  FuType *fuType;
  CData *opcode;
  IData *predTarget;
  CData *compressed;
  Flags *flag;
};

struct ReadRegUop {
  CData *rd;
  CData *prd;
  CData *prs1;
  CData *prs2;
  IData *src1;
  IData *src2;
  CData *robPtr_flag;
  CData *robPtr_index;
  IData *ldqIndex;
  IData *stqIndex;
  IData *imm;
  IData *pc;
  FuType *fuType;
  CData *opcode;
  IData *predTarget;
  CData *compressed;
  Flags *flag;
};

struct WritebackUop {
  CData *prd;
  IData *data;
  CData *robPtr_flag;
  CData *robPtr_index;
  Flags *flag;
  IData *target; // temporary
};

struct CommitUop {
  CData *dest;
  CData *prd;
};

#define V_RENAME_UOP(i, field)                                                 \
  top->rootp->npc_top__DOT__npc__DOT__renameUop_##i##_##field

#define V_WRITEBACK_UOP(i, field)                                              \
  top->rootp->npc_top__DOT__npc__DOT__writebackUop_##i##_bits_##field

#define V_COMMIT_UOP(i, field)                                                 \
  top->rootp->npc_top__DOT__npc__DOT__commitUop_##i##_bits_##field

#define V_READREG_UOP(i, field)                                                \
  top->rootp->npc_top__DOT__npc__DOT__readRegUop_##i##_bits_##field

#define RENAME_FIELDS(X, i)                                                    \
  X(i, rd)                                                                     \
  X(i, prd)                                                                    \
  X(i, prs1)                                                                   \
  X(i, prs2)                                                                   \
  X(i, src1Type)                                                               \
  X(i, src2Type)                                                               \
  X(i, src1Ready)                                                              \
  X(i, src2Ready)                                                              \
  X(i, robPtr_flag)                                                            \
  X(i, robPtr_index)                                                           \
  X(i, ldqIndex)                                                               \
  X(i, stqIndex)                                                               \
  X(i, imm)                                                                    \
  X(i, pc)                                                                     \
  X(i, fuType)                                                                 \
  X(i, opcode)                                                                 \
  X(i, predTarget)                                                             \
  X(i, compressed)                                                             \
  X(i, flag)

#define WRITEBACK_FIELDS(X, i)                                                 \
  X(i, prd)                                                                    \
  X(i, data)                                                                   \
  X(i, robPtr_flag)                                                            \
  X(i, robPtr_index)                                                           \
  X(i, flag)                                                                   \
  X(i, target)

#define COMMIT_FIELDS(X, i)                                                    \
  X(i, dest)                                                                   \
  X(i, prd)                                                                    \
  X(i, robPtr_flag)                                                            \
  X(i, robPtr_index)

#define READREG_FIELDS(X, i)                                                   \
  X(i, rd)                                                                     \
  X(i, prd)                                                                    \
  X(i, prs1)                                                                   \
  X(i, prs2)                                                                   \
  X(i, src1)                                                                   \
  X(i, src2)                                                                   \
  X(i, robPtr_flag)                                                            \
  X(i, robPtr_index)                                                           \
  X(i, ldqIndex)                                                               \
  X(i, stqIndex)                                                               \
  X(i, imm)                                                                    \
  X(i, pc)                                                                     \
  X(i, fuType)                                                                 \
  X(i, opcode)                                                                 \
  X(i, predTarget)                                                             \
  X(i, compressed)                                                             \
  X(i, flag)

#define BIND_ONE(i, field)                                                     \
  UOP[i].field = (decltype(UOP[i].field))&V_UOP(i, field);

#define BIND_FIELDS(i) UOP_FIELDS(BIND_ONE, i)

#define REPEAT_1(FN) FN(0)
#define REPEAT_2(FN) REPEAT_1(FN) FN(1)
#define REPEAT_3(FN) REPEAT_2(FN) FN(2)
#define REPEAT_4(FN) REPEAT_3(FN) FN(3)
#define REPEAT_5(FN) REPEAT_4(FN) FN(4)
#define REPEAT_6(FN) REPEAT_5(FN) FN(5)
#define REPEAT_7(FN) REPEAT_6(FN) FN(6)

#endif