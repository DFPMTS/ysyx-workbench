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
  val iq = Seq.fill(MACHINE_WIDTH)(Module(new IssueQueue))
  val readReg = Module(new ReadReg)
  val pReg = Module(new PReg)
  val alu = Module(new ALU)
    
  val redirect = RegInit(0.U.asTypeOf(new RedirectSignal))

  // * rename
  val renameUop = Wire(Vec(ISSUE_WIDTH, new RenameUop))
  val renameRobValid = Wire(Vec(ISSUE_WIDTH, Bool()))
  val renameIQValid = Wire(Vec(ISSUE_WIDTH, Bool()))
  dontTouch(renameUop)  
  
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
  ifu.io.master.viewAs[AXI4ysyxSoC] <> io.master
  ifu.io.flushICache := false.B

  // * DE
  idu.io.IN_inst <> ifu.io.out
  idu.io.IN_flush := redirect.valid

  // * Rename
  rename.io.IN_decodeUop(0) <> idu.io.OUT_decodeUop
  rename.io.IN_commitUop <> commitUop
  rename.io.IN_writebackUop <> writebackUop
  rename.io.IN_issueQueueReady := scheduler.io.OUT_issueQueueReady
  rename.io.IN_robReady := rob.io.OUT_renameUopReady
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

  rob.io.OUT_redirect <> redirect  
  rob.io.OUT_commitUop <> commitUop


  // * Scheduler
  scheduler.io.IN_issueQueueValid := renameIQValid
  scheduler.io.IN_renameUop := renameUop

  // * Issue Queue
  for (i <- 0 until MACHINE_WIDTH) {
    iq(i).io.IN_renameUop <> scheduler.io.OUT_renameUop(i)
    iq(i).io.IN_writebackUop := writebackUop
    iq(i).io.IN_flush := redirect.valid
  }

  // * Read Register
  readReg.io.IN_issueUop <> iq.map(_.io.OUT_issueUop)
  readReg.io.IN_readRegVal := pReg.io.OUT_pRegVal  
  for (i <- 0 until MACHINE_WIDTH) {
    readReg.io.OUT_readRegUop(i).ready := false.B
  }

  // * PReg
  pReg.io.IN_pRegIndex := readReg.io.OUT_readRegIndex
  pReg.io.IN_writebackUop := writebackUop

  // * Execute
  alu.io.IN_readRegUop <> readReg.io.OUT_readRegUop(0)
  writebackUop(0) := alu.io.OUT_writebackUop

  // * Writeback
  

  // val commitHelper = Module(new CommitHelper);
  // commitHelper.io.commit := mem.io.valid

  // if (Config.debug) {
  //   val valid  = RegNext(mem.io.valid)
  //   val archPC = RegInit(UInt(32.W), Config.resetPC)
  //   archPC := Mux(mem.io.valid, Mux(mem.io.outDnpc.valid, mem.io.outDnpc.pc, archPC + 4.U), archPC)
  //   dontTouch(valid)
  //   dontTouch(archPC)
  // }

  // ifu.io.flushICache := false.B

  // arbiter.io.winMaster.viewAs[AXI4ysyxSoC] <> io.master

  // // AXI4 slave
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