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
  val insert       = Wire(Bool())
  val data_buffer  = RegEnable(io.in.bits, insert)
  val valid_buffer = RegEnable(io.in.valid, insert)

  insert := ~valid_buffer || io.out.fire

  io.in.ready := insert

  val ctrl = data_buffer.ctrl

  // -------------------------- ALU --------------------------
  val alu = Module(new ALU)
  val alu_op1 =
    MuxLookup(ctrl.src1Type, ZERO)(Seq(REG -> data_buffer.rs1, PC -> data_buffer.pc, ZERO -> 0.U))
  val alu_op2 = MuxLookup(ctrl.src2Type, ZERO)(Seq(REG -> data_buffer.rs2, IMM -> data_buffer.imm))
  alu.io.aluFunc := ctrl.aluFunc
  alu.io.op1     := alu_op1
  alu.io.op2     := alu_op2

  io.out.bits.alu_out     := alu.io.out
  io.out.bits.alu_cmp_out := alu.io.cmpOut

  io.out.bits.imm          := data_buffer.imm
  io.out.bits.rs1          := data_buffer.rs1
  io.out.bits.rs2          := data_buffer.rs2
  io.out.bits.ctrl         := data_buffer.ctrl
  io.out.bits.pc           := data_buffer.pc
  io.out.bits.inst         := data_buffer.inst
  io.out.bits.access_fault := data_buffer.access_fault
  io.out.valid             := valid_buffer
}
