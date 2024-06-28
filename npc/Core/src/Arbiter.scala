import chisel3._
import chisel3.util._
import os.stat
import os.truncate

class AXI_Arbiter extends Module {
  val io = IO(new Bundle {
    val IFU_master = Flipped(new AXI4(64, 32))
    val EXU_master = Flipped(new AXI4(64, 32))
    val out_slave  = new AXI4(64, 32)
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

  for (master <- List(io.IFU_master, io.EXU_master)) {
    master.ar.ready := false.B

    master.aw.ready := false.B

    master.w.ready := false.B

    master.r.valid     := false.B
    master.r.bits.data := 0.U
    master.r.bits.resp := 0.U
    master.r.bits.last := true.B
    master.r.bits.id   := 0.U

    master.b.valid     := false.B
    master.b.bits.resp := 0.U
    master.b.bits.id   := 0.U
  }

  for (slave <- List(io.out_slave)) {
    slave.aw.valid      := false.B
    slave.aw.bits.addr  := 0.U
    slave.aw.bits.id    := 0.U
    slave.aw.bits.len   := 0.U
    slave.aw.bits.size  := 0.U
    slave.aw.bits.burst := "b01".U

    slave.w.valid     := false.B
    slave.w.bits.data := 0.U
    slave.w.bits.strb := 0.U
    slave.w.bits.last := true.B

    slave.ar.valid      := false.B
    slave.ar.bits.addr  := 0.U
    slave.ar.bits.id    := 0.U
    slave.ar.bits.len   := 0.U
    slave.ar.bits.size  := 0.U
    slave.ar.bits.burst := "b01".U

    slave.r.ready := false.B
    slave.b.ready := false.B
  }

  switch(state) {
    is(s_IFU) {
      io.IFU_master <> io.out_slave
    }
    is(s_EXU) {
      io.EXU_master <> io.out_slave
    }
  }

}
