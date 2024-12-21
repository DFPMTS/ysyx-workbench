import chisel3._
import chisel3.util._
import utils._

object SrcType extends HasDecodeConfig {
  val ZERO = 0.U(2.W)
  val REG  = 1.U(2.W)
  val IMM  = 2.U(2.W)
  val PC   = 3.U(2.W)
}

object FuType extends HasDecodeConfig {
  val ALU = 0.U(FuTypeWidth.W)
  val BRU = 1.U(FuTypeWidth.W)
  val LSU = 2.U(FuTypeWidth.W)
  val MUL = 3.U(FuTypeWidth.W)
  val DIV = 4.U(FuTypeWidth.W)
  val AGU = 5.U(FuTypeWidth.W)
  val CSR = 6.U(FuTypeWidth.W)
}

object ALUOp extends HasDecodeConfig {
  def ADD = "b0000".U(OpcodeWidth.W)
  def SUB = "b0001".U(OpcodeWidth.W)

  def LEFT  = "b0010".U(OpcodeWidth.W)
  def RIGHT = "b0011".U(OpcodeWidth.W)
  def AND   = "b0100".U(OpcodeWidth.W)
  def OR    = "b0101".U(OpcodeWidth.W)
  def XOR   = "b0110".U(OpcodeWidth.W)
  def ARITH = "b0111".U(OpcodeWidth.W)

  // "b1000"
  // "b1001"

  def EQ = "b1010".U(OpcodeWidth.W)
  def NE = "b1011".U(OpcodeWidth.W)

  def LT  = "b1100".U(OpcodeWidth.W)
  def LTU = "b1101".U(OpcodeWidth.W)

  def GE  = "b1110".U(OpcodeWidth.W)
  def GEU = "b1111".U(OpcodeWidth.W)
}

object BRUOp extends HasDecodeConfig {
  def AUIPC  = "b0000".U(OpcodeWidth.W)
  def JALR   = "b0001".U(OpcodeWidth.W)
  def JAL    = "b0010".U(OpcodeWidth.W)

  def BEQ = "b1010".U(OpcodeWidth.W)
  def BNE = "b1011".U(OpcodeWidth.W)
  def BLT  = "b1100".U(OpcodeWidth.W)
  def BLTU = "b1101".U(OpcodeWidth.W)
  def BGE  = "b1110".U(OpcodeWidth.W)
  def BGEU = "b1111".U(OpcodeWidth.W)
}

object LSUOp extends HasDecodeConfig {
  def LB  = "b0000".U(OpcodeWidth.W)
  def LH  = "b0001".U(OpcodeWidth.W)
  def LW  = "b0010".U(OpcodeWidth.W)
  def LBU = "b0100".U(OpcodeWidth.W)
  def LHU = "b0101".U(OpcodeWidth.W)

  def SB = "b1000".U(OpcodeWidth.W)
  def SH = "b1001".U(OpcodeWidth.W)
  def SW = "b1010".U(OpcodeWidth.W)
}

object CSROp extends HasDecodeConfig {
  def CSRR = "b0000".U(OpcodeWidth.W)
  def CSRRW = "b0001".U(OpcodeWidth.W)
  def CSRRS = "b0010".U(OpcodeWidth.W)
  def CSRRC = "b0011".U(OpcodeWidth.W)

  def CSRRWI = "b0100".U(OpcodeWidth.W)
  def CSRRSI = "b0101".U(OpcodeWidth.W)
  def CSRRCI = "b0110".U(OpcodeWidth.W)

  def SRET = "b1000".U(OpcodeWidth.W)
  def MRET = "b1001".U(OpcodeWidth.W)
  def ECALL = "b1010".U(OpcodeWidth.W)
  def EBREAK = "b1011".U(OpcodeWidth.W)
}

object ImmType extends HasDecodeConfig{
  def I = 0.U(ImmTypeWidth.W)
  def U = 1.U(ImmTypeWidth.W)
  def S = 2.U(ImmTypeWidth.W)
  def B = 3.U(ImmTypeWidth.W)
  def J = 4.U(ImmTypeWidth.W)
  def X = BitPat.dontCare(ImmTypeWidth)
}

object CImmType extends HasDecodeConfig {
  object CI {
    def LWSP = 0.U(ImmTypeWidth.W)
    def ADDI = 1.U(ImmTypeWidth.W)
    def SLLI = 2.U(ImmTypeWidth.W)
    def SLTI = 3.U(ImmTypeWidth.W)
    def ADDI16SP = 4.U(ImmTypeWidth.W)
    def LUI = 5.U(ImmTypeWidth.W)
  }
  object CSS {
    def SW = 6.U(ImmTypeWidth.W)
  }
  def CIW = 7.U(ImmTypeWidth.W)
  object CL {
    def LW = 8.U(ImmTypeWidth.W)
  }
  object CS {
    def SW = 9.U(ImmTypeWidth.W)
  }
  def CA = 10.U(ImmTypeWidth.W)
  object CB {
    def BRANCH = 11.U(ImmTypeWidth.W)
    def SHIFT = 12.U(ImmTypeWidth.W)
    def ANDI = 13.U(ImmTypeWidth.W)
  }
  def CJ = 14.U(ImmTypeWidth.W)
  def X = BitPat.dontCare(ImmTypeWidth)
}


class DecodeUop extends CoreBundle{  
  val rd = UInt(5.W)
  val rs1 = UInt(5.W)
  val rs2 = UInt(5.W)  

  val src1Type = UInt(2.W)
  val src2Type = UInt(2.W)

  val imm = UInt(32.W)
  val pc = UInt(XLEN.W)

  val fuType = UInt(FuTypeWidth.W)
  val opcode = UInt(OpcodeWidth.W)

  val predTarget = UInt(XLEN.W)
  val compressed = Bool()
}

class RenameUop extends CoreBundle {
  val  rd = UInt(5.W)
  val prd = UInt(PREG_IDX_W)

  val prs1 = UInt(PREG_IDX_W)
  val prs2 = UInt(PREG_IDX_W)

  val src1Ready = Bool()
  val src2Ready = Bool()

  val robPtr = RingBufferPtr(ROB_SIZE)
  val ldqIndex = UInt(LDQ_IDX_W)
  val stqIndex = UInt(STQ_IDX_W)

  val imm =  UInt(32.W)  
  val pc = UInt(XLEN.W)
  
  val fuType = UInt(FuTypeWidth.W)
  val opcode = UInt(OpcodeWidth.W)

  val predTarget = UInt(XLEN.W)
  val compressed = Bool()

  val flag = UInt(FLAG_W)
}

class IssueUop extends CoreBundle {
  val  rd = UInt(5.W)
  val prd = UInt(PREG_IDX_W)

  val prs1 = UInt(PREG_IDX_W)
  val prs2 = UInt(PREG_IDX_W)

  val src1Ready = Bool()
  val src2Ready = Bool()

  val robPtr = RingBufferPtr(ROB_SIZE)
  val ldqIndex = UInt(LDQ_IDX_W)
  val stqIndex = UInt(STQ_IDX_W)

  val imm =  UInt(32.W)  
  val pc = UInt(XLEN.W)  
  
  val fuType = UInt(FuTypeWidth.W)
  val opcode = UInt(OpcodeWidth.W)

  val predTarget = UInt(XLEN.W)
  val compressed = Bool()

  val flag = UInt(FLAG_W)
}

class WritebackUop extends CoreBundle {
  val prd = UInt(PREG_IDX_W)
  val data = UInt(XLEN.W)  
  val robPtr = RingBufferPtr(ROB_SIZE)
  val flag = UInt(FLAG_W)
}

class CommitUop extends CoreBundle {
  val rd = UInt(5.W)
  val prd = UInt(PREG_IDX_W)
}