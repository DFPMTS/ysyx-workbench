package utils

import chisel3._
import chisel3.util._

class SRAMArrayIO extends CoreBundle {
  val raddr = Input(UInt(log2Ceil(depth).W))
  val rdata = Output(UInt(width.W))
  val waddr = Input(UInt(log2Ceil(depth).W))
  val wdata = Input(UInt(width.W))
  val wen = Input(Bool())
}

class SRAMArray(val depth: Int, val width: Int) extends Module {
  val io = IO(new Bundle {
    val raddr = Input(UInt(log2Ceil(depth).W))
    val rdata = Output(UInt(width.W))
    val waddr = Input(UInt(log2Ceil(depth).W))
    val wdata = Input(UInt(width.W))
    val wen = Input(Bool())
  })

  val mem = SyncReadMem(depth, Vec(width, UInt(8.W)))

  io.rdata := mem.read(io.raddr, io.wen)(io.raddr)
  when(io.wen) {
    mem.write(io.waddr, VecInit(Seq.fill(width)(io.wdata)), io.wen)
  }
}