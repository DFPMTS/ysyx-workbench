import chisel3._
import chisel3.util._

class ImmGen extends Module {
  val io = IO(new Bundle {
    val inst_type = Input(UInt(3.W));
    val inst      = Input(UInt(32.W));
    val imm       = Output(SInt(32.W))
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

  io.imm := MuxLookup(io.inst_type, 0.S(32.W))(
    Seq("b000".U -> immI, "b001".U -> immU, "b010".U -> immS, "b011".U -> immB, "b100".U -> immJ)
  )
}
