import chisel3._
import chisel3.util._
import utils._

class RenameIO extends CoreBundle {
  // * rename
  val IN_decodeUop = Flipped(Vec(ISSUE_WIDTH, Decoupled(new DecodeUop)))
  val OUT_renameUop = Vec(ISSUE_WIDTH, Decoupled(new RenameUop))
  // * writeback
  val IN_writebackUop = Flipped(Vec(MACHINE_WIDTH, Valid(new WritebackUop)))
  // * commit
  val IN_commitUop = Flipped(Vec(COMMIT_WIDTH, Valid(new CommitUop)))

  val IN_flush     = Input(Bool())
}

class Rename extends CoreModule {
  val io = IO(new RenameIO)

  // * Main signals: renameUop
  val uopReg = Reg(Vec(ISSUE_WIDTH, new RenameUop))
  val uopNext = Wire(Vec(ISSUE_WIDTH, new RenameUop()))
  val uopValid = RegInit(VecInit(Seq.fill(ISSUE_WIDTH)(false.B)))

  // * Submodules
  val renamingTable = Module(new RenamingTable)
  val freeList = Module(new FreeList)

  // * Dataflow

  // ** Decode -> FreeList
  val needPReg = io.IN_decodeUop.map(decodeUop => decodeUop.valid && decodeUop.bits.rd =/= 0.U)
  val renameStall = freeList.io.OUT_renameStall
  for (i <- 0 until ISSUE_WIDTH) {
    // * Allocate PReg
    freeList.io.IN_renameReqValid(i) := needPReg(i)
  }  

  // ** FreeList <- Commit
  for (i <- 0 until COMMIT_WIDTH) {
    freeList.io.IN_commitValid(i) := io.IN_commitUop(i).valid
    freeList.io.IN_commitRd(i) := io.IN_commitUop(i).bits.rd
    freeList.io.IN_commitPReg(i) := io.IN_commitUop(i).bits.prd
    freeList.io.IN_commitPrevPReg(i) := renamingTable.io.OUT_commitPrevPReg(i)
  }

  // ** Decode -> RenamingTable
  for (i <- 0 until ISSUE_WIDTH) {
    // * Read
    renamingTable.io.IN_renameReadAReg(i) := VecInit(io.IN_decodeUop(i).bits.rs1, io.IN_decodeUop(i).bits.rs2)
    // * Write
    renamingTable.io.IN_renameWriteValid(i) := !renameStall && needPReg(i)
    renamingTable.io.IN_renameWriteAReg(i) := io.IN_decodeUop(i).bits.rd
    renamingTable.io.IN_renameWritePReg(i) := freeList.io.OUT_renamePReg(i)
  }

  // ** RenamingTable <- Writeback
  for (i <- 0 until MACHINE_WIDTH) {
    renamingTable.io.IN_writebackValid(i) := io.IN_writebackUop(i).valid
    renamingTable.io.IN_writebackPReg(i) := io.IN_writebackUop(i).bits.prd
  }

  // ** RenamingTable <- Commit
  for (i <- 0 until COMMIT_WIDTH) {
    renamingTable.io.IN_commitValid(i) := io.IN_commitUop(i).valid
    renamingTable.io.IN_commitAReg(i) := io.IN_commitUop(i).bits.rd
    renamingTable.io.IN_commitPReg(i) := io.IN_commitUop(i).bits.prd
  }

  // ** uopNext generation
  for (i <- 0 until ISSUE_WIDTH) {
    uopNext(i).prd := Mux(needPReg(i), freeList.io.OUT_renamePReg(i), 0.U)
    uopNext(i).prs1 := renamingTable.io.OUT_renameReadPReg(i)(0)
    uopNext(i).prs2 := renamingTable.io.OUT_renameReadPReg(i)(1)

    uopNext(i).src1Ready := renamingTable.io.OUT_renameReadReady(i)(0)
    uopNext(i).src2Ready := renamingTable.io.OUT_renameReadReady(i)(0)

    uopNext(i).fuType := io.IN_decodeUop(i).bits.fuType
    uopNext(i).opcode := io.IN_decodeUop(i).bits.opcode

    uopNext(i).imm := io.IN_decodeUop(i).bits.imm
    uopNext(i).pc := io.IN_decodeUop(i).bits.pc    

    uopNext(i).predTarget := io.IN_decodeUop(i).bits.predTarget
    uopNext(i).compressed := io.IN_decodeUop(i).bits.compressed

    uopNext(i).robIndex := 0.U
    uopNext(i).ldqIndex := 0.U
    uopNext(i).stqIndex := 0.U

  }

  // * Control
  val inValid = io.IN_decodeUop.map(_.valid)
  val outReady = io.OUT_renameUop(0).ready
  val inFire = io.IN_decodeUop.map(_.fire)
  val outFire = io.OUT_renameUop(0).fire
  val inReady = (!uopValid.reduce(_ || _) || outReady) && !renameStall


  // ** update uop
  for (i <- 0 until ISSUE_WIDTH) {
    uopReg(i) := Mux(inFire(i), uopNext(i), uopReg(i))
  }

  // ** update uopValid  
  when (io.IN_flush) {
    uopValid := VecInit(Seq.fill(ISSUE_WIDTH)(false.B))
  }.elsewhen(inReady) {
    uopValid := inValid
  }.elsewhen(outFire) {
    uopValid := VecInit(Seq.fill(ISSUE_WIDTH)(false.B))
  }

  // ** Flush submodules
  renamingTable.io.IN_flush := io.IN_flush
  freeList.io.IN_flush := io.IN_flush

  // * Output

  // ** Decode <- Rename
  for (i <- 0 until ISSUE_WIDTH) {
    io.IN_decodeUop(i).ready := inReady
  }  

  // ** Rename -> Issue
  for (i <- 0 until ISSUE_WIDTH) {
    io.OUT_renameUop(i).valid := uopValid(i)
    io.OUT_renameUop(i).bits := uopReg(i)    
  }
}
