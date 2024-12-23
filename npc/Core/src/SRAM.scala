import chisel3._
import chisel3.util._
import os.truncate
import os.stat

// ar.valid and aw.valid MUST NOT be high at the same time
class SRAM extends Module {
  val io = IO(Flipped(new AXI4(32, 32)))

  val read_lat  = 0.U // LFSR16(3)
  val write_lat = 0.U // LFSR16(3)
  val counter   = RegInit(0.U(3.W))

  val addr_buffer = Reg(UInt(32.W))
  val data_buffer = Reg(UInt(32.W))
  val strb_buffer = Reg(UInt(4.W))

  val addr = MuxLookup(Cat(io.ar.fire, io.aw.fire), addr_buffer)(
    Seq("b10".U -> io.ar.bits.addr, "b01".U -> io.aw.bits.addr)
  )
  val data = Mux(io.w.fire, io.w.bits.data, data_buffer)
  val strb = Mux(io.w.fire, io.w.bits.strb, strb_buffer)

  val out_data_buffer = Reg(UInt(32.W))

  val s_Idle :: s_Read :: s_Write :: s_Wait_W :: Nil = Enum(4)

  val next_state = WireDefault(s_Idle)
  val state      = RegNext(next_state, s_Idle)

  next_state := state
  switch(state) {
    is(s_Idle) {
      when(io.ar.fire) {
        next_state := s_Read
      }.elsewhen(io.aw.fire) {
        when(io.w.fire) {
          next_state := s_Write
        }.otherwise {
          next_state := s_Wait_W
        }
      }
    }
    is(s_Read) {
      when(io.r.fire) {
        next_state := s_Idle
      }
    }
    is(s_Wait_W) {
      when(io.w.fire) {
        next_state := s_Write
      }
    }
    is(s_Write) {
      when(io.b.fire) {
        next_state := s_Idle
      }
    }
  }

  switch(next_state) {
    is(s_Read) {
      addr_buffer := io.ar.bits.addr
    }
    is(s_Wait_W) {
      addr_buffer := io.aw.bits.addr
    }
    is(s_Write) {
      addr_buffer := Mux(io.aw.fire, io.aw.bits.addr, addr_buffer)
      data_buffer := io.w.bits.data
      strb_buffer := io.w.bits.strb
    }
  }

  counter := Mux(counter === 0.U, 0.U, counter - 1.U)

  io.ar.ready := false.B
  io.aw.ready := false.B
  io.w.ready  := false.B
  io.r.valid  := false.B
  io.b.valid  := false.B
  switch(state) {
    is(s_Idle) {
      io.ar.ready := true.B
      io.aw.ready := true.B
      io.w.ready  := true.B
    }
    is(s_Read) {
      io.r.valid := counter === 0.U
    }
    is(s_Wait_W) {
      io.w.ready := true.B
    }
    is(s_Write) {
      io.b.valid := counter === 0.U
    }
  }

  val mem_read     = Module(new MemRead)
  val perform_read = state =/= s_Read && next_state === s_Read
  when(perform_read) {
    counter := read_lat
  }
  mem_read.io.clk  := clock
  mem_read.io.addr := addr
  mem_read.io.en   := perform_read
  out_data_buffer  := Mux(perform_read, mem_read.io.data_r, out_data_buffer)

  val mem_write     = Module(new MemWrite)
  val perform_write = state =/= s_Write && next_state === s_Write
  when(perform_write) {
    counter := write_lat
  }
  mem_write.io.clk   := clock
  mem_write.io.addr  := addr
  mem_write.io.wdata := data
  mem_write.io.wmask := strb
  mem_write.io.en    := perform_write

  io.r.bits.resp := 0.U
  io.r.bits.data := out_data_buffer
  io.r.bits.last := true.B
  io.r.bits.id   := 0.U

  io.b.bits.resp := 0.U
  io.b.bits.id   := 0.U
}
