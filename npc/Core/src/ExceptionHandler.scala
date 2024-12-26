import chisel3._
import chisel3.util._
import utils._

class ExceptionHandlerIO extends CoreBundle {
  val IN_exception = Flipped(Valid(new Exception))
  val OUT_flush = Output(Bool())
  val OUT_PC = Output(UInt(32.W))
}


class ExceptionHandler extends CoreModule {
  val io = IO(new ExceptionHandlerIO)

  val flush = RegInit(false.B)
  val PC = Reg(UInt(32.W))

  when(io.IN_exception.valid) {
    flush := true.B
    PC := io.IN_exception.bits.pc
  }

  io.OUT_flush := flush
  io.OUT_PC := PC
}