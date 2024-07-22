import chisel3._
import chisel3.util._
import chisel3.SpecifiedDirection.Flip

class IFU extends Module {
  val io = IO(new Bundle {
    val valid  = Input(Bool())
    val in     = Flipped(new dnpcSignal)
    val out    = Decoupled(new IFU_Message)
    val master = new AXI4(64, 32)
  })
  val pc = RegInit(UInt(32.W), "h80000000".U)

  val insert = Wire(Bool())
  // val dnpcBuffer  = RegEnable(io.in.pc, insert)
  pc := Mux(insert && io.valid, Mux(io.in.valid, io.in.pc, pc + 4.U), pc)
  val validBuffer = RegEnable(io.valid, true.B, insert)
  insert := (~validBuffer || io.out.fire)
  val arValidNext = Wire(Bool())
  val arValid     = RegInit(true.B)
  arValidNext := Mux(insert, io.valid, Mux(io.master.ar.fire, false.B, arValid))
  arValid     := arValidNext

  io.master.ar.valid      := arValid && ~reset.asBool
  io.master.ar.bits.addr  := pc
  io.master.ar.bits.id    := 0.U
  io.master.ar.bits.len   := 0.U
  io.master.ar.bits.size  := 2.U
  io.master.ar.bits.burst := "b01".U
  io.master.r.ready       := io.out.ready

  io.master.aw.valid      := false.B
  io.master.aw.bits.addr  := 0.U
  io.master.aw.bits.id    := 0.U
  io.master.aw.bits.len   := 0.U
  io.master.aw.bits.size  := 0.U
  io.master.aw.bits.burst := "b01".U

  io.master.w.valid     := false.B
  io.master.w.bits.data := 0.U
  io.master.w.bits.strb := 0.U
  io.master.w.bits.last := false.B

  io.master.b.ready := false.B

  val addrOffset = pc(2)
  val retData    = io.master.r.bits.data
  io.out.valid             := io.master.r.valid
  io.out.bits.pc           := pc
  io.out.bits.inst         := Mux(addrOffset, retData(63, 32), retData(31, 0))
  io.out.bits.access_fault := io.master.r.bits.resp =/= 0.U
}
