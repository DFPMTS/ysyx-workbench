import chisel3._
import chisel3.util._
import java.util.function.BiPredicate

trait HasInstType {
  def IMM_I = 0.U(3.W)
  def IMM_U = 1.U(3.W)
  def IMM_S = 2.U(3.W)
  def IMM_B = 3.U(3.W)
  def IMM_J = 4.U(3.W)
  def IMM_R = BitPat("b???")
  def IMM_X = BitPat("b???")
}

class ImmGen extends Module with HasInstType {
  val io = IO(new Bundle {
    val inst_type = Input(UInt(3.W));
    val inst      = Input(UInt(32.W));
    val imm       = Output(UInt(32.W))
  })

  val inst = io.inst;
  val immI = Wire(SInt(32.W))
  val immU = Wire(SInt(32.W))
  val immS = Wire(SInt(32.W))
  val immB = Wire(SInt(32.W))
  val immJ = Wire(SInt(32.W))

  immI := Cat(inst(31, 20)).asSInt
  immU := Cat(inst(31, 12), 0.U(12.W)).asSInt
  immS := Cat(inst(31, 25), inst(11, 7)).asSInt
  immB := Cat(inst(31), inst(7), inst(30, 25), inst(11, 8), 0.U(1.W)).asSInt
  immJ := Cat(inst(31), inst(19, 12), inst(20), inst(30, 21), 0.U(1.W)).asSInt
  io.imm := MuxLookup(io.inst_type, immI)(
    Seq(IMM_I -> immI, IMM_U -> immU, IMM_S -> immS, IMM_B -> immB, IMM_J -> immJ)
  ).asUInt
}
