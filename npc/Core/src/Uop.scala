import chisel3._
import chisel3.util._
import utils.CoreBundle
import java.util.concurrent.Future

object FuType {
  val ALU = 0.U
  val BRU = 1.U
  val LSU = 2.U
  val MUL = 3.U
  val DIV = 4.U
  val AGU = 5.U
  val CSR = 6.U
  def apply() = UInt(4.W)
}

object ALUOp {
  def ADD = "b0000".U
  def SUB = "b0001".U

  def LEFT  = "b0010".U
  def RIGHT = "b0011".U
  def AND   = "b0100".U
  def OR    = "b0101".U
  def XOR   = "b0110".U
  def ARITH = "b0111".U

  // "b1000"
  // "b1001"

  def EQ = "b1010".U
  def NE = "b1011".U

  def LT  = "b1100".U
  def LTU = "b1101".U

  def GE  = "b1110".U
  def GEU = "b1111".U
}

object BRUOp {
  def AUIPC  = "b0000".U
  def JALR   = "b0001".U
  def JAL    = "b0010".U

  def BEQ = "b1010".U
  def BNE = "b1011".U
  def BLT  = "b1100".U
  def BLTU = "b1101".U
  def BGE  = "b1110".U
  def BGEU = "b1111".U
}

object LSUOp {
  def LB  = "b0000".U
  def LH  = "b0001".U
  def LW  = "b0010".U
  def LBU = "b0100".U
  def LHU = "b0101".U

  def SB = "b1000".U
  def SH = "b1001".U
  def SW = "b1010".U
}

object CSROp {
  def CSRR = "b0000".U
  def CSRRW = "b0001".U
  def CSRRS = "b0010".U
  def CSRRC = "b0011".U

  def CSRRWI = "b0100".U
  def CSRRSI = "b0101".U
  def CSRRCI = "b0110".U

  def SRET = "b1000".U
  def MRET = "b1001".U
}


class DecodeUop extends CoreBundle {  
  val rd = UInt(5.W)
  val rs1 = UInt(5.W)
  val rs2 = UInt(5.W)  

  val src1Type = UInt(2.W)
  val src2Type = UInt(2.W)

  val imm = UInt(32.W)
  val pc = UInt(XLEN.W)

  val fuType = FuType()
  val opcode = UInt(7.W)

  val predTarget = UInt(XLEN.W)
  val compressed = Bool()
}

class RenameUop extends CoreBundle {
  val prd = UInt(PREG_IDX_W)

  val prs1 = UInt(PREG_IDX_W)
  val prs2 = UInt(PREG_IDX_W)

  val src1Ready = Bool()
  val src2Ready = Bool()

  val robIndex = UInt(ROB_IDX_W)
  val ldqIndex = UInt(LDQ_IDX_W)
  val stqIndex = UInt(STQ_IDX_W)

  val imm =  UInt(32.W)  
  val pc = UInt(XLEN.W)  
  
  val fuType = FuType()
  val opcode = UInt(7.W)

  val predTarget = UInt(XLEN.W)
  val compressed = Bool()
}

class WritebackUop extends CoreBundle {
  val prd = UInt(PREG_IDX_W)
  val data = UInt(XLEN.W)  
  val robIndex = UInt(ROB_IDX_W)  
  val flag = UInt(FLAG_W)
}

class CommitUop extends CoreBundle {
  val rd = UInt(5.W)
  val data = UInt(XLEN.W)  
  val robIndex = UInt(ROB_IDX_W)  
}