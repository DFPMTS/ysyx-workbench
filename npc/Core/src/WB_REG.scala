import chisel3._
import chisel3.util._

class WB_REG extends Module {
  val io = IO(new Bundle {
    val snpc    = Input(SInt(32.W))
    val alu     = Input(SInt(32.W))
    val mem     = Input(SInt(32.W))
    val wb_sel  = Input(UInt(2.W))
    val wb_data = Output(SInt(32.W))
  })

  io.wb_data := MuxLookup(io.wb_sel, 0.S(32.W))(
    Seq(
      "b00".U -> io.alu,
      "b01".U -> io.mem,
      "b10".U -> io.snpc
    )
  )
}
