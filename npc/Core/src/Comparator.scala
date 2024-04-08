import chisel3._
import chisel3.util._

class Comparator extends Module {
  val io = IO(new Bundle {
    val in1   = Input(SInt(32.W))
    val in2   = Input(SInt(32.W))
    val cmp_U = Input(UInt(1.W))
    val eq    = Output(Bool())
    val lt    = Output(Bool())
    val ge    = Output(Bool())
  })
  io.eq := io.in1 === io.in2
  io.lt := Mux(io.cmp_U.asBool, io.in1.asUInt < io.in2.asUInt, io.in1 < io.in2)
  io.ge := Mux(io.cmp_U.asBool, io.in1.asUInt >= io.in2.asUInt, io.in1 >= io.in2)
}
