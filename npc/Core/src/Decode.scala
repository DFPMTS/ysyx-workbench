import chisel3._
import chisel3.util._
import chisel3.util.experimental._
import chisel3.util.experimental.decode.TruthTable
import chisel3.util.experimental.decode.decoder
import scala.language.implicitConversions

class DecodeSignal extends Bundle {
  val invalid  = Bool()
  val regWe    = Bool()
  val src1Type = UInt(2.W)
  val src2Type = UInt(2.W)
  val aluFunc  = UInt(4.W)
  val fuType   = UInt(2.W)
  val instType = UInt(3.W)
  val fuOp     = UInt(4.W)
}

class Decode extends Module with HasDecodeConstants {
  val io = IO(new Bundle {
    val inst    = Input(UInt(32.W))
    val signals = Output(new DecodeSignal)
  })
  implicit def uintToBitPat(x: UInt): BitPat = BitPat(x)
  def lui        = BitPat("b??????? ????? ????? ??? ????? 01101 11")
  def auipc      = BitPat("b??????? ????? ????? ??? ????? 00101 11")
  def jal        = BitPat("b??????? ????? ????? ??? ????? 11011 11")
  def jalr       = BitPat("b??????? ????? ????? ??? ????? 11001 11")
  def beq        = BitPat("b??????? ????? ????? 000 ????? 11000 11")
  def bne        = BitPat("b??????? ????? ????? 001 ????? 11000 11")
  def blt        = BitPat("b??????? ????? ????? 100 ????? 11000 11")
  def bge        = BitPat("b??????? ????? ????? 101 ????? 11000 11")
  def bltu       = BitPat("b??????? ????? ????? 110 ????? 11000 11")
  def bgeu       = BitPat("b??????? ????? ????? 111 ????? 11000 11")
  def lb         = BitPat("b??????? ????? ????? 000 ????? 00000 11")
  def lh         = BitPat("b??????? ????? ????? 001 ????? 00000 11")
  def lw         = BitPat("b??????? ????? ????? 010 ????? 00000 11")
  def lbu        = BitPat("b??????? ????? ????? 100 ????? 00000 11")
  def lhu        = BitPat("b??????? ????? ????? 101 ????? 00000 11")
  def sb         = BitPat("b??????? ????? ????? 000 ????? 01000 11")
  def sh         = BitPat("b??????? ????? ????? 001 ????? 01000 11")
  def sw         = BitPat("b??????? ????? ????? 010 ????? 01000 11")
  def addi       = BitPat("b??????? ????? ????? 000 ????? 00100 11")
  def slti       = BitPat("b??????? ????? ????? 010 ????? 00100 11")
  def sltiu      = BitPat("b??????? ????? ????? 011 ????? 00100 11")
  def xori       = BitPat("b??????? ????? ????? 100 ????? 00100 11")
  def ori        = BitPat("b??????? ????? ????? 110 ????? 00100 11")
  def andi       = BitPat("b??????? ????? ????? 111 ????? 00100 11")
  def slli       = BitPat("b0000000 ????? ????? 001 ????? 00100 11")
  def srli       = BitPat("b0000000 ????? ????? 101 ????? 00100 11")
  def srai       = BitPat("b0100000 ????? ????? 101 ????? 00100 11")
  def add        = BitPat("b0000000 ????? ????? 000 ????? 01100 11")
  def sub        = BitPat("b0100000 ????? ????? 000 ????? 01100 11")
  def sll        = BitPat("b0000000 ????? ????? 001 ????? 01100 11")
  def slt        = BitPat("b0000000 ????? ????? 010 ????? 01100 11")
  def sltu       = BitPat("b0000000 ????? ????? 011 ????? 01100 11")
  def xor        = BitPat("b0000000 ????? ????? 100 ????? 01100 11")
  def srl        = BitPat("b0000000 ????? ????? 101 ????? 01100 11")
  def sra        = BitPat("b0100000 ????? ????? 101 ????? 01100 11")
  def or         = BitPat("b0000000 ????? ????? 110 ????? 01100 11")
  def and        = BitPat("b0000000 ????? ????? 111 ????? 01100 11")
  def fence_i    = BitPat("b??????? ????? ????? 001 ????? 00011 11")
  def ecall      = BitPat("b0000000 00000 00000 000 00000 11100 11")
  def ebreak     = BitPat("b0000000 00001 00000 000 00000 11100 11")
  def csrrw      = BitPat("b??????? ????? ????? 001 ????? 11100 11")
  def csrrs      = BitPat("b??????? ????? ????? 010 ????? 11100 11")
  def mret       = BitPat("b0011000 00010 00000 000 00000 11100 11")


  val defaultCtrl: List[BitPat] = List(Y, N, ZERO, ZERO, ALU_ADD, ALU, IMM_X, OP_X)
  val lut: List[(BitPat, List[BitPat])] = List(
    lui        -> List(N, Y, ZERO, IMM, ALU_ADD, ALU, IMM_U, OP_X),
    auipc      -> List(N, Y, PC, IMM, ALU_ADD, ALU, IMM_U, OP_X),
    jal        -> List(N, Y, PC, IMM, ALU_ADD, BRU, IMM_J, JUMP),
    jalr       -> List(N, Y, REG, IMM, ALU_ADD, BRU, IMM_I, JUMP),
    beq        -> List(N, N, REG, REG, ALU_EQ, BRU, IMM_B, BRANCH),
    bne        -> List(N, N, REG, REG, ALU_NE, BRU, IMM_B, BRANCH),
    blt        -> List(N, N, REG, REG, ALU_LT, BRU, IMM_B, BRANCH),
    bge        -> List(N, N, REG, REG, ALU_GE, BRU, IMM_B, BRANCH),
    bltu       -> List(N, N, REG, REG, ALU_LTU, BRU, IMM_B, BRANCH),
    bgeu       -> List(N, N, REG, REG, ALU_GEU, BRU, IMM_B, BRANCH),
    lb         -> List(N, Y, REG, IMM, ALU_ADD, MEM, IMM_I, LB),
    lh         -> List(N, Y, REG, IMM, ALU_ADD, MEM, IMM_I, LH),
    lw         -> List(N, Y, REG, IMM, ALU_ADD, MEM, IMM_I, LW),
    lbu        -> List(N, Y, REG, IMM, ALU_ADD, MEM, IMM_I, LBU),
    lhu        -> List(N, Y, REG, IMM, ALU_ADD, MEM, IMM_I, LHU),
    sb         -> List(N, N, REG, IMM, ALU_ADD, MEM, IMM_S, SB ),
    sh         -> List(N, N, REG, IMM, ALU_ADD, MEM, IMM_S, SH),
    sw         -> List(N, N, REG, IMM, ALU_ADD, MEM, IMM_S, SW),
    addi       -> List(N, Y, REG, IMM, ALU_ADD, ALU, IMM_I, OP_X),
    slti       -> List(N, Y, REG, IMM, ALU_LT, ALU, IMM_I, OP_X),
    sltiu      -> List(N, Y, REG, IMM, ALU_LTU, ALU, IMM_I, OP_X),
    xori       -> List(N, Y, REG, IMM, ALU_XOR, ALU, IMM_I, OP_X),
    ori        -> List(N, Y, REG, IMM, ALU_OR, ALU, IMM_I, OP_X),
    andi       -> List(N, Y, REG, IMM, ALU_AND, ALU, IMM_I, OP_X),
    slli       -> List(N, Y, REG, IMM, ALU_LEFT, ALU, IMM_I, OP_X),
    srli       -> List(N, Y, REG, IMM, ALU_RIGHT, ALU, IMM_I, OP_X),
    srai       -> List(N, Y, REG, IMM, ALU_ARITH, ALU, IMM_I, OP_X),
    add        -> List(N, Y, REG, REG, ALU_ADD, ALU, IMM_R, OP_X),
    sub        -> List(N, Y, REG, REG, ALU_SUB, ALU, IMM_R, OP_X),
    sll        -> List(N, Y, REG, REG, ALU_LEFT, ALU, IMM_R, OP_X),
    slt        -> List(N, Y, REG, REG, ALU_LT, ALU, IMM_R, OP_X),
    sltu       -> List(N, Y, REG, REG, ALU_LTU, ALU, IMM_R, OP_X),
    xor        -> List(N, Y, REG, REG, ALU_XOR, ALU, IMM_R, OP_X),
    srl        -> List(N, Y, REG, REG, ALU_RIGHT, ALU, IMM_R, OP_X),
    sra        -> List(N, Y, REG, REG, ALU_ARITH, ALU, IMM_R, OP_X),
    or         -> List(N, Y, REG, REG, ALU_OR, ALU, IMM_R, OP_X),
    and        -> List(N, Y, REG, REG, ALU_AND, ALU, IMM_R, OP_X),
    fence_i    -> List(N, N, ZERO, IMM, ALU_X, CSR, IMM_X, FENCE_I),
    ecall      -> List(N, N, ZERO, ZERO, ALU_X, CSR, IMM_X, ECALL),
    ebreak     -> List(N, N, ZERO, ZERO, ALU_X, CSR, IMM_X, EBREAK),
    csrrw      -> List(N, Y, REG, ZERO, ALU_ADD, CSR, IMM_I, CSRW),
    csrrs      -> List(N, Y, REG, ZERO, ALU_ADD, CSR, IMM_I, CSRS),
    mret       -> List(N, N, ZERO, ZERO, ALU_X, CSR, IMM_X, MRET),
  )
  def listToBitPat(l: List[BitPat]) = {
    l.reduceLeft(_ ## _)
  }

  def transformLUT(lut: List[(BitPat, List[BitPat])]): List[(BitPat, BitPat)] = {
    lut.map {
      case (key, value) =>
        (key, listToBitPat(value))
    }
  }

  val table = TruthTable(transformLUT(lut), listToBitPat(defaultCtrl))
  io.signals := decoder(io.inst, table).asTypeOf(new DecodeSignal)
}
