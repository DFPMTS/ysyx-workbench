import chisel3._
import chisel3.util._

object LFSR16 {
  def apply(len: Int): UInt = {
    require(len < 16)
    val width = 16
    val lfsr  = RegInit(1.U(width.W))
    lfsr := Cat(lfsr(0) ^ lfsr(2) ^ lfsr(3) ^ lfsr(5), lfsr(width - 1, 1))
    lfsr(len - 1, 0)
  }
}
