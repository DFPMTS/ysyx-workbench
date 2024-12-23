
#include <cstdint>

enum class SrcType : uint8_t { ZERO = 0, REG = 1, IMM = 2, PC = 3 };

enum class FuType : uint8_t {
  ALU = 0,
  BRU = 1,
  LSU = 2,
  MUL = 3,
  DIV = 4,
  AGU = 5,
  CSR = 6
};

enum class ALUOp : uint8_t {
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

enum class BRUOp : uint8_t {
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

enum class LSUOp : uint8_t {
  LB = 0b0000,
  LH = 0b0001,
  LW = 0b0010,
  LBU = 0b0100,
  LHU = 0b0101,
  SB = 0b1000,
  SH = 0b1001,
  SW = 0b1010
};

enum class CSROp : uint8_t {
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

enum class ImmType : uint8_t { I = 0, U = 1, S = 2, B = 3, J = 4, X = 0xFF };

enum class CImmType : uint8_t {
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

enum class Flags : uint8_t { NOTHING = 0, MISPREDICT = 1 };
struct RenameUop {
  uint8_t dest;
  uint8_t prd;
  uint8_t prs1;
  uint8_t prs2;
  SrcType src1Type;
  SrcType src2Type;
  bool src1Ready;
  bool src2Ready;
  uint32_t robPtr;
  uint32_t ldqIndex;
  uint32_t stqIndex;
  uint32_t imm;
  uint64_t pc;
  FuType fuType;
  uint8_t opcode;
  uint64_t predTarget;
  bool compressed;
  Flags flag;
};

struct ReadRegUop {
  uint8_t dest;
  uint8_t prd;
  uint8_t prs1;
  uint8_t prs2;
  uint64_t src1;
  uint64_t src2;
  uint32_t robPtr;
  uint32_t ldqIndex;
  uint32_t stqIndex;
  uint32_t imm;
  uint64_t pc;
  FuType fuType;
  uint8_t opcode;
  uint64_t predTarget;
  bool compressed;
  Flags flag;
};

struct WritebackUop {
  uint8_t prd;
  uint64_t data;
  uint32_t robPtr;
  Flags flag;
  uint64_t target; // temporary
};

struct CommitUop {
  uint8_t dest;
  uint8_t prd;
};