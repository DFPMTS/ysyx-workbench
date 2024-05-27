import chisel3._
import chisel3.util._
import os.stat

class AXI_Lite_Arbiter extends Module {
  val io = IO(new Bundle {
    val IFU_master = Flipped(new AXI_Lite)
    val EXU_master = Flipped(new AXI_Lite)
    val out_slave  = new AXI_Lite
  })

  val s_Idle :: s_IFU :: s_EXU :: Nil = Enum(3)

  val next_state = WireDefault(s_Idle)
  val state      = RegNext(next_state, s_Idle)

  val IFU_req = io.IFU_master.ar.valid || io.IFU_master.aw.valid || io.IFU_master.w.valid
  val EXU_req = io.EXU_master.ar.valid || io.EXU_master.aw.valid || io.EXU_master.w.valid

  val IFU_reply = io.IFU_master.r.fire || io.IFU_master.b.fire
  val EXU_reply = io.EXU_master.r.fire || io.EXU_master.b.fire

  next_state := state
  switch(state) {
    is(s_Idle) {
      when(IFU_req) {
        next_state := s_IFU
      }
      when(EXU_req) {
        next_state := s_EXU
      }
    }
    is(s_IFU) {
      when(IFU_reply) { next_state := s_Idle }

    }
    is(s_EXU) {
      when(EXU_reply) { next_state := s_Idle }
    }
  }

  io.IFU_master.ar.ready    := false.B
  io.IFU_master.aw.ready    := false.B
  io.IFU_master.w.ready     := false.B
  io.IFU_master.r.valid     := false.B
  io.IFU_master.r.bits.data := 0.U
  io.IFU_master.r.bits.resp := 0.U
  io.IFU_master.b.valid     := false.B
  io.IFU_master.b.bits.resp := 0.U

  io.EXU_master.ar.ready    := false.B
  io.EXU_master.aw.ready    := false.B
  io.EXU_master.w.ready     := false.B
  io.EXU_master.r.valid     := false.B
  io.EXU_master.r.bits.data := 0.U
  io.EXU_master.r.bits.resp := 0.U
  io.EXU_master.b.valid     := false.B
  io.EXU_master.b.bits.resp := 0.U

  io.out_slave.ar.valid     := false.B
  io.out_slave.ar.bits.addr := 0.U
  io.out_slave.aw.valid     := false.B
  io.out_slave.aw.bits.addr := 0.U
  io.out_slave.w.valid      := false.B
  io.out_slave.w.bits.data  := 0.U
  io.out_slave.w.bits.strb  := 0.U
  io.out_slave.r.ready      := false.B
  io.out_slave.b.ready      := false.B
  switch(state) {
    is(s_IFU) {
      io.IFU_master <> io.out_slave
    }
    is(s_EXU) {
      io.EXU_master <> io.out_slave
    }
  }

}
