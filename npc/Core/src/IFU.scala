import chisel3._
import chisel3.util._
import chisel3.SpecifiedDirection.Flip

class IFUIO extends Bundle {
  // * Invalidate Whole ICache
  val flushICache = Input(Bool())
  val redirect = Input(new RedirectSignal)
  val out = Decoupled(new InstSignal)
  val master = new AXI4(32, 32)
}

class IFU extends Module with HasPerfCounters {
  val io = IO(new IFUIO)

  val icache      = Module(new ICache)

  val pc          = RegInit(UInt(32.W), Config.resetPC)
  val flushBuffer = RegInit(false.B)
  val validBuffer = RegInit(true.B)
  val arValidNext = Wire(Bool())
  val arValid     = RegInit(true.B)

  flushBuffer := Mux(
    io.redirect.valid && !arValid && !icache.io.out.valid,
    true.B,
    Mux(icache.io.out.fire, false.B, flushBuffer)
  )
  val flush    = flushBuffer || io.redirect.valid
  val flushNow = io.redirect.valid && (arValid || icache.io.out.valid)
  val flushFin = flushNow || (flushBuffer && icache.io.out.fire)
  val insert   = Wire(Bool())
  // val dnpcBuffer  = RegEnable(io.in.pc, insert)
  // pc := Mux(insert && io.valid, Mux(io.in.valid, io.in.pc, pc + 4.U), pc)
  val flushPCBuffer = RegEnable(io.redirect.pc, io.redirect.valid)
  val flushPC       = Mux(io.redirect.valid, io.redirect.pc, flushPCBuffer)
  pc := Mux(flushFin, flushPC, Mux(io.out.fire, pc + 4.U, pc))

  insert := (~validBuffer || io.out.fire) || flushFin

  io.master <> icache.io.master
  arValidNext := Mux(insert, true.B, Mux(icache.io.in.fire, false.B, arValid))
  arValid     := arValidNext

  icache.io.in.valid    := arValid && ~reset.asBool && !flush
  icache.io.in.bits     := pc
  icache.io.flushICache := io.flushICache

  val addrOffset = pc(2)
  val retData    = icache.io.out.bits
  io.out.valid             := icache.io.out.valid && !flush
  io.out.bits.pc           := pc
  io.out.bits.inst         := retData
  io.out.bits.access_fault := false.B

  icache.io.out.ready := io.out.ready || flush

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
    val master = new AXI4(32, 32)
  })

  val data = Reg(Vec(4, UInt(32.W)))

  val rCnt = RegInit(0.U(3.W))

  rCnt := Mux(io.in.fire, 0.U, Mux(io.master.r.fire, rCnt + 1.U, rCnt))

  val sIdle :: sFill :: Nil = Enum(2)
  val state                 = RegInit(sIdle)
  state := MuxLookup(state, sIdle)(
    Seq(
      sIdle -> Mux(io.in.valid, sFill, sIdle),
      sFill -> Mux(io.out.fin, sIdle, sFill)
    )
  )

  val isBurst = io.in.bits >= "hA0000000".U && io.in.bits < "hA2000000".U

  val lowAddrBuffer = Reg(UInt(4.W))
  val nextLowAddr   = Mux(!isBurst && io.master.r.valid, lowAddrBuffer + 4.U, lowAddrBuffer)
  lowAddrBuffer := Mux(io.in.fire, 0.U(4.W), nextLowAddr)
  val addr = Cat(io.in.bits(31, 4), lowAddrBuffer)

  val reqValid = Reg(Bool())
  reqValid := Mux(
    io.in.fire,
    true.B,
    Mux(
      io.master.ar.fire,
      false.B,
      Mux(!isBurst && io.master.r.fire, rCnt < 3.U, reqValid)
    )
  )
  io.master.ar.valid      := reqValid && ~reset.asBool
  io.master.ar.bits.addr  := addr
  io.master.ar.bits.id    := 0.U
  io.master.ar.bits.len   := Mux(isBurst, 3.U, 0.U)
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
  io.out.data   := io.master.r.bits.data
  io.out.valid  := io.master.r.valid && state === sFill
  io.out.fin    := state === sFill && rCnt === 4.U
  io.in.ready   := state === sIdle
}

class ICache extends Module with HasPerfCounters {
  val io = IO(new Bundle {
    val in          = Flipped(Decoupled(UInt(32.W)))
    val out         = Decoupled(UInt(32.W))
    val flushICache = Input(Bool())
    val master      = new AXI4(32, 32)
  })
  val numCacheLine = 4

  val data     = Reg(Vec(numCacheLine, Vec(4, UInt(32.W))))
  val tag      = Reg(Vec(numCacheLine, UInt(26.W)))
  val valid    = RegInit(VecInit(Seq.fill(numCacheLine)(false.B)))
  val inTag    = io.in.bits(31, 6)
  val inOffset = io.in.bits(3, 2)
  val inIndex  = io.in.bits(5, 4)

  val fetchLine = Module(new FetchCacheLine)
  io.master <> fetchLine.io.master

  val sIdle :: sFetchReq :: sFetchWaitReply :: Nil = Enum(3)

  val state = RegInit(sIdle)

  fetchLine.io.in.bits  := io.in.bits
  fetchLine.io.in.valid := state === sFetchReq

  val hitMap = valid.zip(tag).map { case (v, t) => v && t === inTag }
  // val hit    = hitMap.reduce(_ || _)
  val hit = valid(inIndex) && tag(inIndex) === inTag

  state := MuxLookup(state, sIdle)(
    Seq(
      sIdle -> Mux(io.in.fire, Mux(hit, sIdle, sFetchReq), sIdle),
      sFetchReq -> Mux(fetchLine.io.in.fire, sFetchWaitReply, sFetchReq),
      sFetchWaitReply -> Mux(io.out.fire, sIdle, sFetchWaitReply)
    )
  )

  when(io.flushICache) {
    valid.foreach(_ := false.B)
  }

  val replaceIdx = RegInit(0.U(2.W))
  replaceIdx := Mux(replaceIdx === 2.U, replaceIdx, replaceIdx + 1.U)

  when(fetchLine.io.out.valid) {
    data(inIndex)(fetchLine.io.out.offset) := fetchLine.io.out.data
  }
  when(fetchLine.io.out.fin) {
    tag(inIndex)   := inTag
    valid(inIndex) := true.B
  }

  io.in.ready := state === sIdle && !io.flushICache

  val line = Mux1H(hitMap, data)
  // io.out.bits := line(inOffset)
  io.out.bits := data(inIndex)(inOffset)

  io.out.valid := hit

  monitorEvent(icacheMiss, state === sIdle && !hit)
}

class FullyAssocICache extends Module with HasPerfCounters {
  val io = IO(new Bundle {
    val in     = Flipped(Decoupled(UInt(32.W)))
    val out    = Decoupled(UInt(32.W))
    val master = new AXI4(32, 32)
  })
  val numCacheLine = 3

  val data     = Reg(Vec(numCacheLine, Vec(4, UInt(32.W))))
  val tag      = Reg(Vec(numCacheLine, UInt(28.W)))
  val valid    = RegInit(VecInit(Seq.fill(numCacheLine)(false.B)))
  val inTag    = io.in.bits(31, 4)
  val inOffset = io.in.bits(3, 2)
  val inIndex  = io.in.bits(5, 4)

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

  val hasInvalid = !valid.reduce(_ && _)
  val invalidIdx = PriorityMux(valid.zipWithIndex.map { case (v, i) => (!v -> i.U) })
  val rndIdxReg  = RegInit(0.U(2.W))
  val rndIdx     = RegEnable(rndIdxReg, state === sCheckHit && !hit)
  val replaceIdx = Mux(hasInvalid, invalidIdx, rndIdx)
  rndIdxReg := Mux(rndIdxReg === numCacheLine.U - 1.U, 0.U, rndIdxReg + 1.U)

  when(fetchLine.io.out.valid) {
    data(replaceIdx)(fetchLine.io.out.offset) := fetchLine.io.out.data
  }
  when(fetchLine.io.out.fin) {
    tag(replaceIdx)   := inTag
    valid(replaceIdx) := true.B
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
