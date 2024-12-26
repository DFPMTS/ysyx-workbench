import chisel3._
import chisel3.util._
import utils._

class FlagHandlerIO extends CoreBundle {
  val IN_exception = Flipped(Valid(new FlagUop))
  val OUT_flush = Output(Bool())
  val OUT_redirect = Output(new RedirectSignal)  
}


class FlagHandler extends CoreModule {
  val io = IO(new FlagHandlerIO)

  val flush = RegInit(false.B)
  val PC = Reg(UInt(32.W))

  when(io.IN_exception.valid) {
    flush := true.B
    PC := io.IN_exception.bits.pc
  }

  io.OUT_flush := flush
  io.OUT_PC := PC
}