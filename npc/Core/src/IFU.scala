import chisel3._
import chisel3.util._

class IFU extends Module {
  val io = IO(new Bundle {
    val in     = Flipped(Decoupled(new PC_Message))
    val out    = Decoupled(new IFU_Message)
    val master = new AXI_Lite
  })

  // val sram      = Module(new SRAM)
  val pc_buffer = RegEnable(io.in.bits.pc, io.master.ar.fire)

  io.master.ar.valid     := io.in.valid
  io.master.ar.bits.addr := io.in.bits.pc
  io.master.r.ready      := io.out.ready

  io.master.aw.valid     := false.B
  io.master.aw.bits.addr := 0.U
  io.master.w.valid      := false.B
  io.master.w.bits.data  := 0.U
  io.master.w.bits.strb  := 0.U
  io.master.b.ready      := false.B

  io.in.ready      := io.master.ar.ready
  io.out.valid     := io.master.r.valid
  io.out.bits.pc   := pc_buffer
  io.out.bits.inst := io.master.r.bits.data
  // switch(state) {
  //   is(s_Idle) {
  //     when(insert) {
  //       when(sram.io.ar.fire) {}
  //     }
  //   }
  //   is(s_WaitSReq) {}
  //   is(s_WaitSReply) {}
  // }

  // val inst_fetch = Module(new InstFetch)
  // inst_fetch.io.pc := data_buffer.pc
  // io.out.bits.inst := inst_fetch.io.inst
  // io.out.bits.pc   := data_buffer.pc
}
