import chisel3._
import chisel3.util._

class MEM extends Module {
  val io = IO(new Bundle {
    val addr   = Input(UInt(32.W))
    val mr     = Input(UInt(1.W))
    val mw     = Input(UInt(1.W))
    val len    = Input(UInt(2.W))
    val load_U = Input(UInt(1.W))
    val data_r = Output(SInt(32.W))
    val data_w = Input(SInt(32.W))
  })
  val mem_read    = Module(new MemRead)
  val mem_write   = Module(new MemWrite)
  val addr_offset = io.addr(1, 0)
  val len_mask = MuxLookup(io.len, 0.U(4.W))(
    Seq(
      0.U(2.W) -> "b0001".U,
      1.U(2.W) -> "b0011".U,
      2.U(2.W) -> "b1111".U
    )
  )
  val mask = len_mask << addr_offset
  mem_read.io.addr := io.addr
  mem_read.io.en   := io.mr
  val raw_data = mem_read.io.data_r >> (addr_offset << 3.U)

  io.data_r := raw_data
  when(io.len === "b00".U) {
    io.data_r := Cat(Fill(24, ~io.load_U & raw_data(7)), raw_data(7, 0)).asSInt
  }.elsewhen(io.len === "b01".U) {
    io.data_r := Cat(Fill(16, ~io.load_U & raw_data(15)), raw_data(15, 0)).asSInt
  }

  mem_write.io.addr  := io.addr
  mem_write.io.wdata := io.data_w << (addr_offset << 3.U)
  mem_write.io.en    := io.mw
  mem_write.io.wmask := mask
}

class MemRead extends HasBlackBoxPath {
  val io = IO(new Bundle {
    val addr   = Input(UInt(32.W))
    val en     = Input(UInt(1.W))
    val data_r = Output(SInt(32.W))
  })
  addPath("Core/src/MemRead.v")
}

class MemWrite extends HasBlackBoxPath {
  val io = IO(new Bundle {
    val addr  = Input(UInt(32.W))
    val wdata = Input(SInt(32.W))
    val en    = Input(UInt(1.W))
    val wmask = Input(UInt(4.W))
  })
  addPath("Core/src/MemWrite.v")
}
