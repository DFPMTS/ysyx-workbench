import chisel3._
import chisel3.util._
import utils._
import Config.XLEN

object CSRList {
  val mstatus   = 0x300.U
  val mtvec     = 0x305.U
  val mepc      = 0x341.U
  val mcause    = 0x342.U
  val mvendorid = 0xf11.U
  val marchid   = 0xf12.U
}

class CSRIO extends CoreBundle {
  val IN_readRegUop  = Flipped(Decoupled(new ReadRegUop))
  val OUT_writebackUop  = Valid(new WritebackUop)
}

class CSR extends Module {
  val io = IO(new CSRIO)
  val mstatus = RegInit("h1800".U(32.W))
  val mtvec   = Reg(UInt(32.W))
  val mepc    = Reg(UInt(32.W))
  val mcause  = Reg(UInt(32.W))

  val inValid = io.IN_readRegUop.valid
  val inUop = io.IN_readRegUop.bits
  val addr = io.IN_readRegUop.bits.imm
  val ren  = inValid
  val wen  = inValid && inUop.opcode === CSROp.CSRRW
  val wdata = inUop.src1
  val rdata = Wire(UInt(XLEN.W))
  val ecall = inValid && inUop.opcode === CSROp.ECALL

  val uop = Reg(new WritebackUop)
  val uopValid = RegInit(false.B)

  when(ecall) {
    // M-mode
    mcause := 11.U
    mepc   := inUop.pc
  }
  rdata := 0.U
  when(ren) {
    when(addr === CSRList.mstatus) {
      rdata := mstatus
    }
    when(addr === CSRList.mtvec) {
      rdata := mtvec
    }
    when(addr === CSRList.mepc) {
      rdata := mepc
    }
    when(addr === CSRList.mcause) {
      rdata := mcause
    }
    when(addr === CSRList.mvendorid) {
      rdata := 0x79737978.U
    }
    when(addr === CSRList.marchid) {
      rdata := 23060238.U
    }
  }

  when(wen) {
    when(addr === CSRList.mstatus) {
      mstatus := wdata
    }
    when(addr === CSRList.mtvec) {
      mtvec := wdata
    }
    when(addr === CSRList.mepc) {
      mepc := wdata
    }
    when(addr === CSRList.mcause) {
      mcause := wdata
    }
  }

  uopValid := inValid
  uop.data := rdata
  uop.target := Mux(ecall, mtvec, 0.U)
  uop.flag := Mux(ecall, Flags.MISPREDICT, Flags.NOTHING)
  uop.prd  := inUop.prd
  uop.robPtr := inUop.robPtr
  
  io.OUT_writebackUop.valid := uopValid
  io.OUT_writebackUop.bits := uop
}
