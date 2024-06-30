import chisel3._
import chisel3.util._

class Error extends HasBlackBoxPath {
  val io = IO(new Bundle {
    val ebreak       = Input(UInt(1.W))
    val access_fault = Input(UInt(1.W))
    val invalid_inst = Input(UInt(1.W))
  })

  addPath("Core/src/Error.v")
}
