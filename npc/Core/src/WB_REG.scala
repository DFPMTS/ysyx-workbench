import chisel3._
import chisel3.util._

class WB_REG extends Module {
  val io = IO(new Bundle {
    val snpc    = Input(UInt(32.W))
    val alu     = Input(UInt(32.W))
    val mem     = Input(UInt(32.W))
    val csr     = Input(UInt(32.W))
    val wb_sel  = Input(UInt(2.W))
    val wb_data = Output(UInt(32.W))
  })

  io.wb_data := MuxLookup(io.wb_sel, 0.U(32.W))(
    Seq(
      "b00".U -> io.alu,
      "b01".U -> io.mem,
      "b10".U -> io.snpc,
      "b11".U -> io.csr
    )
  )
}
