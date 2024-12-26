import chisel3._
import chisel3.util._
import utils._

trait HasLSUOps {
  def U    = 0.U(1.W)
  def S    = 1.U(1.W)
  def BYTE = 0.U(2.W)
  def HALF = 1.U(2.W)
  def WORD = 2.U(2.W)
  def R    = 0.U(1.W)
  def W    = 1.U(1.W)

  def LB  = BitPat("b0000")
  def LBU = BitPat("b0001")

  def LH  = BitPat("b0010")
  def LHU = BitPat("b0011")

  def LW = BitPat("b0100")

  def SB = BitPat("b1000")

  def SH = BitPat("b1010")

  def SW = BitPat("b1100")
}

class LSUIO extends CoreBundle {
  val IN_readRegUop = Flipped(Decoupled(new ReadRegUop))
  val OUT_writebackUop = Valid(new WritebackUop)
  val master = new AXI4(32, 32)
}

class LSU extends CoreModule with HasLSUOps {
  val io = IO(new LSUIO)

  val sIdle :: sWaitResp :: Nil = Enum(2)
  val state = RegInit(sIdle)

  val inUop = io.IN_readRegUop.bits
  val opcode = inUop.opcode

  val insert = state === sIdle && io.IN_readRegUop.valid
  val respValid = io.master.r.fire || io.master.b.fire
  io.IN_readRegUop.ready := state === sWaitResp && respValid

  state := MuxLookup(state, sIdle)(
    Seq(
      sIdle -> Mux(io.IN_readRegUop.valid, sWaitResp, sIdle),
      sWaitResp -> Mux(io.master.r.fire || io.master.b.fire, sIdle, sWaitResp)
    )
  )

  val memLen     = opcode(2, 1)
  val loadU      = opcode(0)
  val is_read_w  = opcode(3) === R
  val is_write_w = opcode(3) === W

  val addr        = RegEnable(inUop.src1 + inUop.imm, insert)
  val addr_offset = addr(1, 0)

  // ar_valid/aw_valid/w_valid 当一个valid请求进入时置为true,在相应通道握手后为false
  val ar_valid = RegInit(false.B)
  ar_valid := Mux(
    insert,
    is_read_w,
    Mux(io.master.ar.fire, false.B, ar_valid)
  )
  io.master.ar.valid      := ar_valid
  io.master.ar.bits.addr  := addr
  io.master.ar.bits.id    := 0.U
  io.master.ar.bits.len   := 0.U
  io.master.ar.bits.size  := memLen
  io.master.ar.bits.burst := "b01".U

  val rdata = io.master.r.bits.data
  io.master.r.ready := true.B

  val aw_valid = RegInit(false.B)
  aw_valid := Mux(
    insert,
    is_write_w,
    Mux(io.master.aw.fire, false.B, aw_valid)
  )
  io.master.aw.valid      := aw_valid
  io.master.aw.bits.addr  := addr
  io.master.aw.bits.id    := 0.U
  io.master.aw.bits.len   := 0.U
  io.master.aw.bits.size  := memLen
  io.master.aw.bits.burst := "b01".U

  val w_valid = RegInit(false.B)
  w_valid := Mux(
    insert,
    is_write_w,
    Mux(io.master.w.fire, false.B, w_valid)
  )
  io.master.w.valid     := w_valid
  io.master.w.bits.data := inUop.src2 << (addr_offset << 3.U)
  io.master.w.bits.strb := MuxLookup(memLen, 0.U(4.W))(
    Seq(
      0.U(2.W) -> "b0001".U,
      1.U(2.W) -> "b0011".U,
      2.U(2.W) -> "b1111".U
    )
  ) << addr_offset
  io.master.w.bits.last := true.B

  io.master.b.ready := true.B

  val raw_data      = rdata >> (addr_offset << 3.U)
  val sign_ext_data = WireDefault(raw_data)
  when(memLen === BYTE) {
    sign_ext_data := Cat(Fill(24, ~loadU & raw_data(7)), raw_data(7, 0))
  }.elsewhen(memLen === HALF) {
    sign_ext_data := Cat(Fill(16, ~loadU & raw_data(15)), raw_data(15, 0))
  }

  val uop = Reg(new WritebackUop)
  val uopValid = RegInit(false.B)
  
  uopValid := io.master.r.fire || io.master.b.fire
  
  uop.data := sign_ext_data
  uop.prd := inUop.prd
  uop.robPtr := inUop.robPtr
  uop.flag := 0.U
  uop.target := 0.U

  io.OUT_writebackUop.bits := uop
  io.OUT_writebackUop.valid := uopValid
}