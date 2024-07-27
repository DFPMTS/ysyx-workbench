import chisel3._
import chisel3.util._
import chisel3.SpecifiedDirection.Flip

class IFU extends Module with HasPerfCounters {
  val io = IO(new Bundle {
    val valid  = Input(Bool())
    val in     = Flipped(new dnpcSignal)
    val out    = Decoupled(new IFU_Message)
    val master = new AXI4(64, 32)
  })
  val pc = RegInit(UInt(32.W), Config.resetPC)

  val insert = Wire(Bool())
  // val dnpcBuffer  = RegEnable(io.in.pc, insert)
  pc := Mux(insert && io.valid, Mux(io.in.valid, io.in.pc, pc + 4.U), pc)
  val validBuffer = RegEnable(io.valid, true.B, insert)
  insert := (~validBuffer || io.out.fire)
  val arValidNext = Wire(Bool())
  val arValid     = RegInit(true.B)

  val icache = Module(new ICache)

  arValidNext := Mux(insert, io.valid, Mux(icache.io.in.fire, false.B, arValid))
  arValid     := arValidNext

  icache.io.in.valid := arValid && ~reset.asBool
  icache.io.in.bits  := pc

  val addrOffset = pc(2)
  val retData    = icache.io.out.bits
  io.out.valid             := icache.io.out.valid
  io.out.bits.pc           := pc
  io.out.bits.inst         := retData
  io.out.bits.access_fault := false.B

  icache.io.out.ready := io.out.ready

  io.master <> icache.io.master

  monitorEvent(ifuFinished, io.out.fire)
  monitorEvent(ifuStalled, validBuffer && ~reset.asBool)
}

class FetchCacheLine extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(UInt(32.W)))
    val out = new Bundle {
      val offset = UInt(2.W)
      val data   = UInt(32.W)
      val valid  = Bool()
      val fin    = Bool()
    }
    val master = new AXI4(64, 32)
  })

  val data = Reg(Vec(4, UInt(32.W)))

  val rCnt = RegInit(0.U(3.W))

  rCnt := Mux(io.in.fire, 0.U, Mux(io.master.r.fire, rCnt + 1.U, rCnt))
  val nextAddr   = Wire(UInt(32.W))
  val addrBuffer = Reg(UInt(32.W))
  addrBuffer := Mux(io.in.fire, Cat(io.in.bits(31, 4), 0.U(4.W)), nextAddr)

  val sIdle :: sFill :: Nil = Enum(2)
  val state                 = RegInit(sIdle)
  state := MuxLookup(state, sIdle)(
    Seq(
      sIdle -> Mux(io.in.valid, sFill, sIdle),
      sFill -> Mux(io.out.fin, sIdle, sFill)
    )
  )

  nextAddr := Mux(io.master.r.valid, addrBuffer + 4.U, addrBuffer)

  val reqValid = Reg(Bool())
  reqValid := Mux(
    io.in.fire,
    true.B,
    Mux(
      io.master.ar.fire,
      false.B,
      Mux(io.master.r.fire, rCnt < 3.U, reqValid)
    )
  )
  io.master.ar.valid      := reqValid && ~reset.asBool
  io.master.ar.bits.addr  := addrBuffer
  io.master.ar.bits.id    := 0.U
  io.master.ar.bits.len   := 0.U
  io.master.ar.bits.size  := 2.U
  io.master.ar.bits.burst := "b01".U

  io.master.r.ready := true.B

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

  io.out.offset := rCnt
  io.out.data   := Mux(addrBuffer(2), io.master.r.bits.data(63, 32), io.master.r.bits.data(31, 0))
  io.out.valid  := io.master.r.valid
  io.out.fin    := state === sFill && rCnt === 4.U
  io.in.ready   := state === sIdle
}

class ICache extends Module with HasPerfCounters {
  val io = IO(new Bundle {
    val in     = Flipped(Decoupled(UInt(32.W)))
    val out    = Decoupled(UInt(32.W))
    val master = new AXI4(64, 32)
  })
  val numCacheLine = 3

  val data     = Reg(Vec(numCacheLine, Vec(4, UInt(32.W))))
  val tag      = Reg(Vec(numCacheLine, UInt(28.W)))
  val valid    = RegInit(VecInit(Seq.fill(numCacheLine)(false.B)))
  val inTag    = io.in.bits(31, 4)
  val inOffset = io.in.bits(3, 2)

  val fetchLine = Module(new FetchCacheLine)
  io.master <> fetchLine.io.master

  val sIdle :: sCheckHit :: sFetchLine :: Nil = Enum(3)

  val state = RegInit(sIdle)

  fetchLine.io.in.bits  := io.in.bits
  fetchLine.io.in.valid := state === sFetchLine

  val hitMap = valid.zip(tag).map { case (v, t) => v && t === inTag }
  val hit    = hitMap.reduce(_ || _)

  state := MuxLookup(state, sIdle)(
    Seq(
      sIdle -> Mux(io.in.valid, sCheckHit, sIdle),
      sCheckHit -> Mux(hit, Mux(io.out.fire, sIdle, sCheckHit), sFetchLine),
      sFetchLine -> Mux(fetchLine.io.out.fin, sCheckHit, sFetchLine)
    )
  )

  val replaceIdx = RegInit(0.U(2.W))
  replaceIdx := Mux(replaceIdx === 2.U, replaceIdx, replaceIdx + 1.U)

  when(fetchLine.io.out.valid) {
    data(0)(fetchLine.io.out.offset) := fetchLine.io.out.data
    for (i <- 0 until (numCacheLine - 1)) {
      data(i + 1)(fetchLine.io.out.offset) := data(i)(fetchLine.io.out.offset)
    }
  }
  when(fetchLine.io.out.fin) {
    tag(0)   := inTag
    valid(0) := true.B
    for (i <- 0 until (numCacheLine - 1)) {
      valid(i + 1) := valid(i)
      tag(i + 1)   := tag(i)
    }
  }

  io.in.ready := state === sIdle

  io.out.bits := Mux1H(hitMap, data)(inOffset)

  io.out.valid := state === sCheckHit && hit

  monitorEvent(icacheMiss, state === sCheckHit && !hit)
}

// class ICacheTest extends Module {
//   val io = IO(new Bundle {
//     val in  = Input(UInt(32.W))
//     val out = UInt(32.W)
//   })

//   val x      = Reg(UInt(32.W))
//   val icache = Module(new ICache)
//   icache.io.in    := x ^ io.in
//   x               := icache.io.out
//   icache.io.wdata := x & io.in
//   icache.io.win   := x | io.in
//   io.out          := x
// }
