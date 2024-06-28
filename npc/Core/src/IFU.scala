import chisel3._
import chisel3.util._

class IFU extends Module {
  val io = IO(new Bundle {
    val in     = Flipped(Decoupled(new PC_Message))
    val out    = Decoupled(new IFU_Message)
    val master = new AXI4(64, 32)
  })

  val pc        = io.in.bits.pc
  val pc_buffer = RegEnable(pc, io.master.ar.fire)

  io.master.ar.valid      := io.in.valid
  io.master.ar.bits.addr  := pc
  io.master.ar.bits.id    := 0.U
  io.master.ar.bits.len   := 0.U
  io.master.ar.bits.size  := 2.U
  io.master.ar.bits.burst := "b01".U
  io.master.r.ready       := io.out.ready

  io.master.aw.valid      := false.B
  io.master.aw.bits.addr  := 0.U
  io.master.aw.bits.id    := 0.U
  io.master.aw.bits.len   := 0.U
  io.master.aw.bits.size  := 0.U
  io.master.aw.bits.burst := "b01".U

  io.master.w.valid     := false.B
  io.master.w.bits.data := 0.U
  io.master.w.bits.strb := 0.U
  io.master.w.bits.last := false.B

  io.master.b.ready := false.B

  val addr_offset = pc_buffer(2)
  val ret_data    = io.master.r.bits.data
  io.in.ready      := io.master.ar.ready
  io.out.valid     := io.master.r.valid
  io.out.bits.pc   := pc_buffer
  io.out.bits.inst := Mux(addr_offset, ret_data(63, 32), ret_data(31, 0))
}
