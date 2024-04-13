import chisel3._
import chisel3.util._

class WB_PC extends Module {
  val io = IO(new Bundle {
    val snpc        = Input(UInt(32.W))
    val mtvec       = Input(UInt(32.W))
    val ecall       = Input(Bool())
    val mepc        = Input(UInt(32.W))
    val mret        = Input(Bool())
    val target      = Input(UInt(32.W))
    val take_branch = Input(Bool())
    val jal         = Input(Bool())
    val jalr        = Input(Bool())
    val dnpc        = Output(UInt(32.W))
  })
  io.dnpc := io.snpc
  when(io.ecall) {
    io.dnpc := io.mtvec
  }
  when(io.mret) {
    io.dnpc := io.mepc
  }
  when(io.take_branch || io.jal || io.jalr) {
    io.dnpc := io.target
  }
}
