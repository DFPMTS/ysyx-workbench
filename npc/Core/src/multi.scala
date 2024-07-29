import chisel3._
import chisel3.util._
import chisel3.experimental.dataview._

class multi extends Module {
  val io = IO(new Bundle {
    val master    = new AXI4ysyxSoC(32, 32)
    val slave     = Flipped(new AXI4ysyxSoC(32, 32))
    val interrupt = Input(Bool())
  })

  val ifu = Module(new IFU)
  val idu = Module(new IDU)
  val exu = Module(new EXU)
  val mem = Module(new MEM)
  val wbu = Module(new WBU)

  // val pc = RegInit(UInt(32.W), "h80000000".U)
  // val pc = RegInit(UInt(32.W), "h0ff00000".U)
  val arbiter = Module(new AXI_Arbiter)
  // val xbar    = Module(new XBar)

  val WBtoIF = Wire(new dnpcSignal)
  val WBtoDE = Wire(new WBSignal)

  // IF
  ifu.io.in.pc    := WBtoIF.pc
  ifu.io.in.valid := WBtoIF.valid && !reset.asBool // ! IFU must latch pc/valid inside
  ifu.io.master <> arbiter.io.IFUMaster

  // DE
  idu.io.in <> ifu.io.out
  idu.io.wb := WBtoDE

  // EX
  exu.io.in <> idu.io.out

  // MEM
  mem.io.in <> exu.io.out
  mem.io.master <> arbiter.io.LSUMaster
  // mem.io.master <> xbar.io.in
  // xbar.io.out <> arbiter.io.LSUMaster

  // WB
  wbu.io.in <> mem.io.out
  ifu.io.flushICache := wbu.io.flushICache
  WBtoIF             := wbu.io.dnpc
  WBtoDE             := wbu.io.wb
  ifu.io.valid       := wbu.io.valid

  // valid
  val wbuValid = RegNext(wbu.io.valid)
  dontTouch(wbuValid)

  // val error = Module(new Error)
  // error.io.ebreak       := 0.U
  // error.io.access_fault := 0.U
  // error.io.invalid_inst := 0.U
  // when(wbu.io.out.valid) {
  //   pc_valid_r            := true.B
  //   pc                    := wbu.io.out.bits.dnpc
  //   regfile.io.reg_we     := wbu.io.out.bits.reg_we
  //   error.io.ebreak       := wbu.io.out.bits.ebreak
  //   error.io.access_fault := wbu.io.out.bits.access_fault
  //   error.io.invalid_inst := wbu.io.out.bits.invalid_inst
  // }
  // val commit_inst = RegNext(wbu.io.out.bits.inst)
  // dontTouch(commit_inst)

  // AXI4 master
  // arbiter.io.winMaster <> xbar.io.in
  arbiter.io.winMaster.viewAs[AXI4ysyxSoC] <> io.master
  // xbar.io.out.viewAs[AXI4ysyxSoC] <> io.master

  // AXI4 slave
  io.slave.awready := false.B
  io.slave.arready := false.B
  io.slave.wready  := false.B

  io.slave.bvalid := false.B
  io.slave.bresp  := 0.U
  io.slave.bid    := 0.U

  io.slave.rdata  := 0.U
  io.slave.rvalid := false.B
  io.slave.rresp  := 0.U
  io.slave.rid    := 0.U
  io.slave.rlast  := true.B
}
