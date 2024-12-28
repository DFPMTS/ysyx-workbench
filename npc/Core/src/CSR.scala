import chisel3._
import chisel3.util._
import utils._
import Config.XLEN

object CSRList {
  val sstatus        = 0x100.U
  val sie            = 0x104.U
  val stvec          = 0x105.U
  val scounteren     = 0x106.U

  val sscratch       = 0x140.U
  val sepc           = 0x141.U
  val scause         = 0x142.U
  val stval          = 0x143.U
  val sip            = 0x144.U
  val satp           = 0x180.U

  val mstatus        = 0x300.U
  val misa           = 0x301.U
  val medeleg        = 0x302.U
  val mideleg        = 0x303.U
  val mie            = 0x304.U
  val mtvec          = 0x305.U
  val mcounteren     = 0x306.U
  val menvcfg        = 0x30A.U
  val mstatush       = 0x310.U
  val menvcfgh       = 0x31A.U
  val mscratch       = 0x340.U
  val mepc           = 0x341.U
  val mcause         = 0x342.U
  val mtval          = 0x343.U
  val mip            = 0x344.U

  val time           = 0xC01.U
  val timeh          = 0xC81.U

  val mvendorid      = 0xF11.U
  val marchid        = 0xF12.U
  val mipid          = 0xF13.U
  val mhartid        = 0xF14.U
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
  val ebreak = inValid && inUop.opcode === CSROp.EBREAK

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

  val error = Module(new Error)
  error.io.ebreak := ebreak
  error.io.access_fault := false.B
  error.io.invalid_inst := false.B

  uopValid := inValid
  uop.data := rdata
  uop.target := Mux(ecall, mtvec, 0.U)
  uop.flag := Mux(ecall, Flags.MISPREDICT, Flags.NOTHING)
  uop.prd  := inUop.prd
  uop.robPtr := inUop.robPtr
  
  io.IN_readRegUop.ready := true.B

  io.OUT_writebackUop.valid := uopValid
  io.OUT_writebackUop.bits := uop
}
