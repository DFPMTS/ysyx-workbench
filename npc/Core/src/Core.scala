import chisel3._
import chisel3.util._
import utils._
import chisel3.experimental.dataview._

class Core extends CoreModule {
  val io = IO(new Bundle {
    val master    = new AXI4ysyxSoC(32, 32)
    val slave     = Flipped(new AXI4ysyxSoC(32, 32))
    val interrupt = Input(Bool())
  })

  val ifu = Module(new IFU)
  val idu = Module(new IDU)
  val rename = Module(new Rename)
  val rob = Module(new ROB)
  val scheduler = Module(new Scheduler)
  val iq = Seq(
    Module(new IssueQueue(Seq(FuType.ALU, FuType.CSR))),
    Module(new IssueQueue(Seq(FuType.LSU))),
    Module(new IssueQueue(Seq(FuType.ALU))),
    Module(new IssueQueue(Seq(FuType.ALU))),
  )
  val dispatcher = Seq(
    Module(new Dispatcher(Seq(Seq(FuType.ALU, FuType.BRU), Seq(FuType.CSR)))),
  )

  val readReg = Module(new ReadReg)
  val pReg = Module(new PReg)
  val alu = Module(new ALU)
  val lsu = Module(new LSU)
  val csr = Module(new CSR)

  val arbiter = Module(new AXI_Arbiter)

  val redirect = RegInit(0.U.asTypeOf(new RedirectSignal))

  // * rename
  val renameUop = Wire(Vec(ISSUE_WIDTH, new RenameUop))
  val renameRobValid = Wire(Vec(ISSUE_WIDTH, Bool()))
  val renameRobReady = Wire(Bool())
  val renameIQValid = Wire(Vec(ISSUE_WIDTH, Bool()))
  dontTouch(renameUop)  
  dontTouch(renameRobValid)
  dontTouch(renameRobReady)
  dontTouch(renameIQValid)
  
  // * read register
  val readRegUop = Wire(Vec(MACHINE_WIDTH, Decoupled(new ReadRegUop)))
  dontTouch(readRegUop)

  // * writeback
  val writebackUop = Wire(Vec(MACHINE_WIDTH, Valid(new WritebackUop)))
  for (i <- 0 until MACHINE_WIDTH) {
    writebackUop(i).valid := false.B
    writebackUop(i).bits := 0.U.asTypeOf(new WritebackUop)
  }
  dontTouch(writebackUop)

  // * commit
  val commitUop = Wire(Vec(COMMIT_WIDTH, Valid(new CommitUop)))
  dontTouch(commitUop)  

  // * IF
  ifu.io.redirect := redirect
  arbiter.io.winMaster.viewAs[AXI4ysyxSoC] <> io.master
  ifu.io.flushICache := false.B

  // * DE
  idu.io.IN_inst <> ifu.io.out
  idu.io.IN_flush := redirect.valid

  // * Rename
  rename.io.IN_decodeUop(0) <> idu.io.OUT_decodeUop
  rename.io.IN_commitUop <> commitUop
  rename.io.IN_writebackUop <> writebackUop
  rename.io.IN_issueQueueReady := scheduler.io.OUT_issueQueueReady
  rename.io.IN_robReady := renameRobReady
  rename.io.IN_flush := redirect.valid

  rename.io.OUT_renameUop <> renameUop
  rename.io.OUT_robValid <> renameRobValid
  rename.io.OUT_issueQueueValid <> renameIQValid

  // * ROB
  rob.io.IN_renameRobHeadPtr := rename.io.OUT_robHeadPtr
  for (i <- 0 until ISSUE_WIDTH) {
    rob.io.IN_renameUop(i).valid := renameRobValid(i)
    rob.io.IN_renameUop(i).bits := renameUop(i)
  }  
  rob.io.IN_writebackUop <> writebackUop
  rob.io.IN_flush := redirect.valid

  rob.io.OUT_renameUopReady <> renameRobReady
  rob.io.OUT_redirect <> redirect  
  rob.io.OUT_commitUop <> commitUop

  // * Scheduler
  scheduler.io.IN_issueQueueValid := renameIQValid
  scheduler.io.IN_renameUop := renameUop

  // * Issue Queue
  for (i <- 0 until MACHINE_WIDTH) {
    iq(i).io.IN_renameUop <> scheduler.io.OUT_renameUop(i)
    iq(i).io.IN_writebackUop := writebackUop
    iq(i).io.IN_robTailPtr := rob.io.OUT_robTailPtr
    iq(i).io.IN_flush := redirect.valid
  }

  // * Read Register
  readReg.io.IN_issueUop <> iq.map(_.io.OUT_issueUop)
  readReg.io.IN_readRegVal := pReg.io.OUT_pRegVal
  for (i <- 0 until MACHINE_WIDTH) {    
    readRegUop(i) <> readReg.io.OUT_readRegUop(i)
    readRegUop(i).ready := false.B
  }

  // * PReg
  pReg.io.IN_pRegIndex := readReg.io.OUT_readRegIndex
  pReg.io.IN_writebackUop := writebackUop

  // * Execute
  // ** Port 0: ALU / CSR
  dispatcher(0).io.IN_uop <> readRegUop(0)

  alu.io.IN_readRegUop <> dispatcher(0).io.OUT_uop(0)
  csr.io.IN_readRegUop <> dispatcher(0).io.OUT_uop(1)

  val port0wbsel = Module(new WritebackSel(2))
  port0wbsel.io.IN_uop(0) := alu.io.OUT_writebackUop
  port0wbsel.io.IN_uop(1) := csr.io.OUT_writebackUop
  writebackUop(0) := port0wbsel.io.OUT_uop

  // ** Port 1: LSU

  lsu.io.IN_readRegUop <> readRegUop(1)
  writebackUop(1) := lsu.io.OUT_writebackUop

  // * AXI4 master
  arbiter.io.IFUMaster <> ifu.io.master
  arbiter.io.LSUMaster <> lsu.io.master

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