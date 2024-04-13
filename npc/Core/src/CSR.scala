import chisel3._
import chisel3.util._

object CSRList {
  val mstatus = 0x300.U
  val mtvec   = 0x305.U
  val mepc    = 0x341.U
  val mcause  = 0x342.U
}

class CSR extends Module {
  val io = IO(
    new Bundle {
      val ren   = Input(Bool())
      val addr  = Input(UInt(12.W))
      val wen   = Input(Bool())
      val wtype = Input(UInt(2.W))
      val wdata = Input(UInt(32.W))
      val ecall = Input(Bool())
      val pc    = Input(UInt(32.W))

      val rdata  = Output(UInt(32.W))
      val mepc   = Output(UInt(32.W))
      val mcause = Output(UInt(32.W))
      val mtvec  = Output(UInt(32.W))
    }
  )
  val mstatus = RegInit("h1800".U(32.W))
  val mtvec   = Reg(UInt(32.W))
  val mepc    = Reg(UInt(32.W))
  val mcause  = Reg(UInt(32.W))
  io.mtvec  := mtvec
  io.mepc   := mepc
  io.mcause := mcause

  when(io.ecall) {
    // M-mode
    mcause := 11.U
    mepc   := io.pc
  }
  io.rdata := 0.U
  when(io.ren) {
    when(io.addr === CSRList.mstatus) {
      io.rdata := mstatus
    }
    when(io.addr === CSRList.mtvec) {
      io.rdata := mtvec
    }
    when(io.addr === CSRList.mepc) {
      io.rdata := mepc
    }
    when(io.addr === CSRList.mcause) {
      io.rdata := mcause
    }
  }

  val write = "b01".U(2.W)
  val set   = "b10".U(2.W)
  val clear = "b11".U(2.W)
  when(io.wen) {
    when(io.wtype === write) {
      when(io.addr === CSRList.mstatus) {
        mstatus := io.wdata
      }
      when(io.addr === CSRList.mtvec) {
        mtvec := io.wdata
      }
      when(io.addr === CSRList.mepc) {
        mepc := io.wdata
      }
      when(io.addr === CSRList.mcause) {
        mcause := io.wdata
      }
    }
  }
}
