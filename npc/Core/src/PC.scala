import chisel3._
import chisel3.util._

class PC extends Module {
  val io = IO(new Bundle {
    val dnpc = Input(UInt(32.W))
    val pc   = Output(UInt(32.W))
    val snpc = Output(UInt(32.W))
  })

  val pc = RegInit("h80000000".U(32.W))
  pc := io.dnpc

  io.snpc := pc + 4.U
  io.pc   := pc
}
