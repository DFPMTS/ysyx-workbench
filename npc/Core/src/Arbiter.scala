import chisel3._
import chisel3.util._
import os.stat
import os.truncate

// class AXI_Arbiter extends Module {
//   val io = IO(new Bundle {
//     val IFUMaster = Flipped(new AXI4(32, 32))
//     val LSUMaster = Flipped(new AXI4(32, 32))
//     val winMaster = new AXI4(32, 32)
//   })

//   val sIdle :: sIFU :: sLSU :: Nil = Enum(3)

//   val nextState = WireDefault(sIdle)
//   val state     = RegNext(nextState, sIdle)

//   val IFUreq = io.IFUMaster.ar.valid || io.IFUMaster.aw.valid || io.IFUMaster.w.valid
//   val LSUreq = io.LSUMaster.ar.valid || io.LSUMaster.aw.valid || io.LSUMaster.w.valid

//   val IFUreply = (io.IFUMaster.r.fire && io.IFUMaster.r.bits.last) || io.IFUMaster.b.fire
//   val LSUreply = (io.LSUMaster.r.fire && io.LSUMaster.r.bits.last) || io.LSUMaster.b.fire
//   // 注意这里有问题：假设一个master能发出多个请求（如ar通道多次握手），当第一个请求得到回复后就会直接变为Idle状态，这是错误的
//   // 修改方法1：让ar aw w仅能握手一次，即在ar aw w握手后将其ready置为false
//   nextState := MuxLookup(state, sIdle)(
//     Seq(
//       sIdle -> Mux(LSUreq, sLSU, Mux(IFUreq, sIFU, sIdle)),
//       sIFU -> Mux(IFUreply, sIdle, sIFU),
//       sLSU -> Mux(LSUreply, sIdle, sLSU)
//     )
//   )

//   for (master <- List(io.IFUMaster, io.LSUMaster)) {
//     master.ar.ready := false.B

//     master.aw.ready := false.B

//     master.w.ready := false.B

//     master.r.valid     := false.B
//     master.r.bits.data := 0.U
//     master.r.bits.resp := 0.U
//     master.r.bits.last := true.B
//     master.r.bits.id   := 0.U

//     master.b.valid     := false.B
//     master.b.bits.resp := 0.U
//     master.b.bits.id   := 0.U
//   }

//   for (slave <- List(io.winMaster)) {
//     slave.aw.valid      := false.B
//     slave.aw.bits.addr  := 0.U
//     slave.aw.bits.id    := 0.U
//     slave.aw.bits.len   := 0.U
//     slave.aw.bits.size  := 0.U
//     slave.aw.bits.burst := "b01".U

//     slave.w.valid     := false.B
//     slave.w.bits.data := 0.U
//     slave.w.bits.strb := 0.U
//     slave.w.bits.last := true.B

//     slave.ar.valid      := false.B
//     slave.ar.bits.addr  := 0.U
//     slave.ar.bits.id    := 0.U
//     slave.ar.bits.len   := 0.U
//     slave.ar.bits.size  := 0.U
//     slave.ar.bits.burst := "b01".U

//     slave.r.ready := false.B
//     slave.b.ready := false.B
//   }
//   // val win = WireDefault(io.LSUMaster)
//   switch(state) {
//     is(sIFU) {
//       // io.IFUMaster <> win
//       io.winMaster <> io.IFUMaster
//     }
//     is(sLSU) {
//       // io.LSUMaster <> win
//       io.winMaster <> io.LSUMaster
//     }
//   }
// }

class AXI_Arbiter extends Module {
  val io = IO(new Bundle {
    val IFUMaster = Flipped(new AXI4(32, 32))
    val LSUMaster = Flipped(new AXI4(32, 32))
    val winMaster = new AXI4(32, 32)
  })

  val sIdle :: sIFU :: sLSU :: Nil = Enum(3)

  val nextState = WireDefault(sIdle)
  val state     = RegNext(nextState, sIdle)

  val IFUreq = io.IFUMaster.ar.valid || io.IFUMaster.aw.valid || io.IFUMaster.w.valid
  val LSUreq = io.LSUMaster.ar.valid || io.LSUMaster.aw.valid || io.LSUMaster.w.valid

  val IFUreply = (io.IFUMaster.r.fire && io.IFUMaster.r.bits.last) || io.IFUMaster.b.fire
  val LSUreply = (io.LSUMaster.r.fire && io.LSUMaster.r.bits.last) || io.LSUMaster.b.fire
  // 注意这里有问题：假设一个master能发出多个请求（如ar通道多次握手），当第一个请求得到回复后就会直接变为Idle状态，这是错误的
  // 修改方法1：让ar aw w仅能握手一次，即在ar aw w握手后将其ready置为false
  nextState := MuxLookup(state, sIdle)(
    Seq(
      sIdle -> Mux(LSUreq, sLSU, Mux(IFUreq, sIFU, sIdle)),
      sIFU -> Mux(IFUreply, sIdle, sIFU),
      sLSU -> Mux(LSUreply, sIdle, sLSU)
    )
  )

  for (master <- List(io.IFUMaster, io.LSUMaster)) {
    master.ar.ready := false.B

    master.aw.ready := false.B

    master.w.ready := false.B

    master.r.valid     := false.B
    master.r.bits.data := 0.U
    master.r.bits.resp := 0.U
    master.r.bits.last := true.B
    master.r.bits.id   := 0.U

    master.b.valid     := false.B
    master.b.bits.resp := 0.U
    master.b.bits.id   := 0.U
  }

  val win = Wire(Flipped(new AXI4(32, 32)))
  for (slave <- List(win)) {
    slave.aw.valid      := false.B
    slave.aw.bits.addr  := 0.U
    slave.aw.bits.id    := 0.U
    slave.aw.bits.len   := 0.U
    slave.aw.bits.size  := 0.U
    slave.aw.bits.burst := "b01".U

    slave.w.valid     := false.B
    slave.w.bits.data := 0.U
    slave.w.bits.strb := 0.U
    slave.w.bits.last := true.B

    slave.ar.valid      := false.B
    slave.ar.bits.addr  := 0.U
    slave.ar.bits.id    := 0.U
    slave.ar.bits.len   := 0.U
    slave.ar.bits.size  := 0.U
    slave.ar.bits.burst := "b01".U

    slave.r.ready := false.B
    slave.b.ready := false.B
  }

  switch(state) {
    is(sIFU) {
      // io.IFUMaster <> win
      win <> io.IFUMaster
    }
    is(sLSU) {
      // io.LSUMaster <> win
      win <> io.LSUMaster
    }
  }

  // io.winMaster <> win
  // toIn :>= win
  // val toWin = WireDefault(win)

  val mtime = RegInit(0.U(64.W))
  mtime := mtime + 1.U
  val CLINT_BASE = 0x02000000
  val CLINT_SIZE = 0x00000008

  val toOut = WireDefault(win)
  val toIn  = WireDefault(io.winMaster)

  // read CLINT
  val readCLINT    = win.ar.bits.addr >= CLINT_BASE.U && win.ar.bits.addr < (CLINT_BASE + CLINT_SIZE).U
  val readCLINTReg = RegInit(false.B)
  val isUpper      = RegEnable(win.ar.bits.addr(2), win.ar.valid)

  readCLINTReg := Mux(
    win.ar.valid && readCLINT,
    true.B,
    Mux(win.r.ready, false.B, readCLINTReg)
  )
  toOut.ar.valid   := win.ar.valid && !readCLINT
  toIn.ar.ready    := Mux(readCLINT, true.B, io.winMaster.ar.ready)
  toIn.r.valid     := Mux(readCLINTReg, true.B, io.winMaster.r.valid)
  toIn.r.bits.last := Mux(readCLINTReg, true.B, io.winMaster.r.bits.last)
  toIn.r.bits.data := Mux(readCLINTReg, Mux(isUpper, mtime(63, 32), mtime(31, 0)), io.winMaster.r.bits.data)

  // toWin <> toIn
  toIn :>= win
  // dontTouch(readCLINT)
  // dontTouch(readCLINTReg)
  // dontTouch(win)
  // dontTouch(toIn)
  // io.winMaster :>= toIn
  io.winMaster :<= toOut

}
