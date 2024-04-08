import chisel3._
import chisel3.util._

class WB_PC extends Module {
  val io = IO(new Bundle {
    val snpc    = Input(UInt(32.W))
    val alu     = Input(UInt(32.W))
    val is_beq  = Input(UInt(1.W))
    val is_bne  = Input(UInt(1.W))
    val is_blt  = Input(UInt(1.W))
    val is_bge  = Input(UInt(1.W))
    val is_jump = Input(UInt(1.W))
    val eq      = Input(Bool())
    val lt      = Input(Bool())
    val ge      = Input(Bool())
    val dnpc    = Output(UInt(32.W))
  })
  io.dnpc := io.snpc
  when(
    (io.is_beq.asBool && io.eq) || (io.is_bne.asBool && !io.eq) || (io.is_blt.asBool && io.lt) || (io.is_bge.asBool && io.ge) || io.is_jump.asBool
  ) {
    io.dnpc := io.alu
  }
}
