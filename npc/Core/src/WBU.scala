import chisel3._
import chisel3.util._
import dataclass.data
import Config.debug

trait HasCSROps {
  def CSRW   = 0.U(4.W)
  def CSRS   = 1.U(4.W)
  def ECALL  = 2.U(4.W)
  def MRET   = 3.U(4.W)
  def EBREAK = 4.U(4.W)
}

class WBU extends Module with HasDecodeConstants {
  val io = IO(new Bundle {
    val in    = Flipped(Decoupled(new MEM_Message))
    val wb    = new WBSignal
    val dnpc  = new dnpcSignal
    val valid = Output(Bool())
  })
  val valid       = io.in.valid
  val validBuffer = RegNext(valid)
  val wbBuffer    = RegEnable(io.in.bits.wb, valid)
  val dnpcBuffer  = RegEnable(io.in.bits.dnpc, valid)

  io.in.ready := true.B

  io.wb.wen  := validBuffer && wbBuffer.wen
  io.wb.data := wbBuffer.data
  io.wb.rd   := wbBuffer.rd

  io.dnpc.valid := validBuffer && dnpcBuffer.valid
  io.dnpc.pc    := dnpcBuffer.pc

  io.valid := validBuffer

  if (Config.debug) {
    val ctrlBuffer = RegNext(io.in.bits.ctrl)
    val dataBuffer = RegNext(io.in.bits.data)
    dontTouch(ctrlBuffer)
    dontTouch(dataBuffer)
  }
}
