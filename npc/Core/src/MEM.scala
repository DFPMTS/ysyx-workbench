import chisel3._
import chisel3.util._

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

class MEM extends Module with HasDecodeConstants {
  val io = IO(new Bundle {
    val in     = Flipped(Decoupled(new EXU_Message))
    val out    = Decoupled(new MEM_Message)
    val master = new AXI4(64, 32)
  })

  val insert      = Wire(Bool())
  val dataBuffer  = RegEnable(io.in.bits.data, insert)
  val ctrlBuffer  = RegEnable(io.in.bits.ctrl, insert)
  val dnpcBuffer  = RegEnable(io.in.bits.dnpc, insert)
  val validBuffer = RegEnable(io.in.valid, insert)
  insert      := ~validBuffer || io.out.fire
  io.in.ready := insert

  val memOut  = Wire(UInt(32.W))
  val WBOut   = Wire(new WBSignal)
  val dnpcOut = Wire(new dnpcSignal)

  // -------------------------- MEM --------------------------
  val memLen     = ctrlBuffer.fuOp(2, 1)
  val loadU      = ctrlBuffer.fuOp(0)
  val ctrl_w     = io.in.bits.ctrl
  val invalid    = ctrl_w.invalid
  val is_mem_w   = ctrl_w.fuType === MEM
  val is_read_w  = is_mem_w && ctrl_w.fuOp(3) === R
  val is_write_w = is_mem_w && ctrl_w.fuOp(3) === W

  val invalidBuffer = RegEnable(invalid, insert)
  val is_mem        = RegEnable(is_mem_w, insert)
  val is_read       = RegEnable(is_read_w, insert)
  val is_write      = RegEnable(is_write_w, insert)

  // ar_valid/aw_valid/w_valid 当一个valid请求进入时置为true,在相应通道握手后为false
  val ar_valid = RegInit(false.B)
  ar_valid := Mux(
    insert,
    io.in.valid && is_read_w && !invalid,
    Mux(io.master.ar.fire, false.B, ar_valid)
  )
  val addr        = dataBuffer.out
  val addr_offset = addr(2, 0);
  io.master.ar.valid      := ar_valid
  io.master.ar.bits.addr  := addr
  io.master.ar.bits.id    := 0.U
  io.master.ar.bits.len   := 0.U
  io.master.ar.bits.size  := memLen
  io.master.ar.bits.burst := "b01".U

  val rValidBuffer = RegNext(io.master.r.valid)
  val rdataBuffer  = RegNext(io.master.r.bits.data)
  io.master.r.ready := Mux(validBuffer && is_read, io.out.ready, false.B)

  val aw_valid = RegInit(false.B)
  aw_valid := Mux(
    insert,
    io.in.valid && is_write_w && !invalid,
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
    io.in.valid && is_write_w && !invalid,
    Mux(io.master.w.fire, false.B, w_valid)
  )
  io.master.w.valid     := w_valid
  io.master.w.bits.data := dataBuffer.rs2Val << (addr_offset << 3.U)
  io.master.w.bits.strb := MuxLookup(memLen, 0.U(4.W))(
    Seq(
      0.U(2.W) -> "b0001".U,
      1.U(2.W) -> "b0011".U,
      2.U(2.W) -> "b1111".U
    )
  ) << addr_offset
  io.master.w.bits.last := true.B

  val bValidBuffer = RegNext(io.master.b.valid)
  io.master.b.ready := Mux(validBuffer && is_write, io.out.ready, false.B)

  val raw_data      = rdataBuffer >> (addr_offset << 3.U)
  val sign_ext_data = WireDefault(raw_data)
  when(memLen === BYTE) {
    sign_ext_data := Cat(Fill(24, ~loadU & raw_data(7)), raw_data(7, 0))
  }.elsewhen(memLen === HALF) {
    sign_ext_data := Cat(Fill(16, ~loadU & raw_data(15)), raw_data(15, 0))
  }

  memOut := sign_ext_data
  // ---------------------------------------------------------

  WBOut.data := Mux(is_mem, memOut, dataBuffer.out)
  WBOut.rd   := ctrlBuffer.rd
  WBOut.wen  := ctrlBuffer.regWe

  dnpcOut.valid := dnpcBuffer.valid
  dnpcOut.pc    := dnpcBuffer.pc

  io.out.bits.ctrl := ctrlBuffer
  io.out.bits.data := dataBuffer
  io.out.bits.dnpc := dnpcOut
  io.out.bits.wb   := WBOut
  io.out.valid := Mux(
    is_mem.asBool && !invalidBuffer,
    Mux(is_read, rValidBuffer, bValidBuffer),
    validBuffer
  )
}
