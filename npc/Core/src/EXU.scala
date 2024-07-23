import chisel3._
import chisel3.util._
import dataclass.data

trait HasFuTypes {
  def ALU = 0.U(2.W)
  def BRU = 1.U(2.W)
  def MEM = 2.U(2.W)
  def CSR = 3.U(2.W)
}

trait HasBRUOps {
  def JUMP   = 0.U(4.W)
  def BRANCH = 1.U(4.W)
}

class EXU extends Module with HasDecodeConstants {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new IDU_Message))
    val out = Decoupled(new EXU_Message)
  })
  val insert      = Wire(Bool())
  val ctrlBuffer  = RegEnable(io.in.bits.ctrl, insert)
  val dataBuffer  = RegEnable(io.in.bits.data, insert)
  val validBuffer = RegEnable(io.in.valid, insert)

  insert := ~validBuffer || io.out.fire

  io.in.ready := insert

  val ctrlOut = WireInit(ctrlBuffer)
  val dataOut = WireInit(dataBuffer)
  val dnpcOut = Wire(new dnpcSignal)

  // -------------------- Function Units ---------------------

  val aluOut        = Wire(UInt(32.W))
  val aluCmpOut     = Wire(Bool())
  val aluJumpTarget = Wire(UInt(32.W))

  val bruOut     = Wire(UInt(32.W))
  val bruPC      = Wire(UInt(32.W))
  val bruPCValid = Wire(Bool())

  // -------------------------- ALU --------------------------
  val alu = Module(new ALU)
  alu.io.aluFunc := ctrlBuffer.aluFunc
  // alu.io.op1     := MuxLookup(ctrlBuffer.src1Type, 0.U)(Seq(REG -> dataBuffer.src1, PC -> dataBuffer.pc, ZERO -> 0.U))
  // alu.io.op2     := MuxLookup(ctrlBuffer.src2Type, 0.U)(Seq(REG -> dataBuffer.src2, IMM -> dataBuffer.imm, ZERO -> 0.U))
  alu.io.op1    := dataBuffer.src1
  alu.io.op2    := dataBuffer.src2
  aluOut        := alu.io.out
  aluCmpOut     := alu.io.cmpOut
  aluJumpTarget := alu.io.jumpTarget

  // -------------------------- BRU --------------------------
  val isBRU  = ctrlBuffer.fuType === BRU
  val isJUMP = ctrlBuffer.fuOp === JUMP

  bruOut     := dataBuffer.pc + 4.U
  bruPCValid := isJUMP || aluCmpOut
  bruPC      := Mux(isJUMP, aluJumpTarget, dataBuffer.pc + dataBuffer.imm)

  // ---------------------------------------------------------

  ctrlOut := ctrlBuffer

  dataOut.out := Mux(ctrlBuffer.fuType === BRU, bruOut, aluOut)

  dnpcOut.valid := isBRU && bruPCValid
  dnpcOut.pc    := bruPC

  io.out.bits.ctrl := ctrlOut
  io.out.bits.data := dataOut
  io.out.bits.dnpc := dnpcOut
  io.out.valid     := validBuffer
}
