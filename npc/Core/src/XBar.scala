import chisel3._
import chisel3.util._

class XBar extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(new AXI4(32, 32))
    val out = new AXI4(32, 32)
  })
  val mtime = RegInit(0.U(64.W))
  mtime := mtime + 1.U
  val CLINT_BASE = 0x02000000
  val CLINT_SIZE = 0x00000008

  val toOut = WireDefault(io.in)
  val toIn  = WireDefault(io.out)

  // read CLINT
  val readCLINT    = io.in.ar.bits.addr >= CLINT_BASE.U && io.in.ar.bits.addr < (CLINT_BASE + CLINT_SIZE).U
  val readCLINTReg = RegInit(false.B)
  val isUpper      = RegEnable(io.in.ar.bits.addr(2), io.in.ar.valid)

  readCLINTReg := Mux(
    io.in.ar.valid && readCLINT,
    true.B,
    Mux(io.in.r.ready, false.B, readCLINTReg)
  )
  toOut.ar.valid   := io.in.ar.valid && !readCLINT
  toIn.ar.ready    := Mux(readCLINT, true.B, io.out.ar.ready)
  toIn.r.valid     := Mux(readCLINTReg, true.B, io.out.r.valid)
  toIn.r.bits.last := Mux(readCLINTReg, true.B, io.out.r.bits.last)
  toIn.r.bits.data := Mux(readCLINTReg, Mux(isUpper, mtime(63, 32), mtime(31, 0)), io.out.r.bits.data)

  io.in <> toIn
  io.out <> toOut
}
