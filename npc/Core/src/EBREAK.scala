import chisel3._
import chisel3.util._

class EBREAK extends HasBlackBoxPath {
  val io = IO(new Bundle {
    val ebreak = Input(UInt(1.W))
  })

  addPath("Core/src/EBREAK.v")
}
