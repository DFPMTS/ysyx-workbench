import chisel3._
import chisel3.util._
import utils._

class RenameIO extends CoreBundle {
  // * rename
  val IN_decodeUop = Flipped(Vec(ISSUE_WIDTH, Decoupled(new DecodeUop)))
  val OUT_renameUop = Vec(ISSUE_WIDTH, new RenameUop)
  
  val OUT_robValid = Vec(ISSUE_WIDTH, Output(Bool()))
  val IN_robReady = Flipped(Bool())

  val OUT_issueQueueValid = Vec(ISSUE_WIDTH, Output(Bool()))  
  val IN_issueQueueReady = Flipped(Vec(ISSUE_WIDTH, Bool()))
  // * writeback
  val IN_writebackUop = Flipped(Vec(MACHINE_WIDTH, Valid(new WritebackUop)))
  // * commit
  val IN_commitUop = Flipped(Vec(COMMIT_WIDTH, Valid(new CommitUop)))
  val OUT_robHeadPtr = Output(RingBufferPtr(ROB_SIZE))
  val IN_flush     = Input(Bool())
}

class Rename extends CoreModule {
  val io = IO(new RenameIO)

  // * Main signals: renameUop
  val uopReg = Reg(Vec(ISSUE_WIDTH, new RenameUop))
  val uopNext = Wire(Vec(ISSUE_WIDTH, new RenameUop))
  val uopRobValid = RegInit(VecInit(Seq.fill(ISSUE_WIDTH)(false.B)))
  val uopIssueQueueValid = RegInit(VecInit(Seq.fill(ISSUE_WIDTH)(false.B)))

  // * Submodules
  val renamingTable = Module(new RenamingTable)
  val freeList = Module(new FreeList)

  // * Dataflow

  // ** Decode -> FreeList
  val allocatePReg = io.IN_decodeUop.map(decodeUop => decodeUop.fire && decodeUop.bits.rd =/= 0.U)
  val renameStall = freeList.io.OUT_renameStall
  for (i <- 0 until ISSUE_WIDTH) {
    // * Allocate PReg
    freeList.io.IN_renameReqValid(i) := allocatePReg(i)
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
    renamingTable.io.IN_renameWriteValid(i) := allocatePReg(i)
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

  // ** robPtr allocation
  val robHeadPtr = RegInit(RingBufferPtr(size = ROB_SIZE, flag = 0.U, index = 0.U))
  when (io.IN_flush) {
    robHeadPtr := RingBufferPtr(size = ROB_SIZE, flag = 0.U, index = 0.U)
  }.otherwise {
    robHeadPtr := robHeadPtr + PopCount(io.IN_decodeUop.map(_.fire))  
  }
  io.OUT_robHeadPtr := robHeadPtr
  
  // ** uopNext generation
  for (i <- 0 until ISSUE_WIDTH) {
    val decodeUop = io.IN_decodeUop(i).bits

    uopNext(i).rd := decodeUop.rd
    uopNext(i).prd := Mux(allocatePReg(i), freeList.io.OUT_renamePReg(i), 0.U)
    uopNext(i).prs1 := renamingTable.io.OUT_renameReadPReg(i)(0)
    uopNext(i).prs2 := renamingTable.io.OUT_renameReadPReg(i)(1)

    uopNext(i).src1Type := decodeUop.src1Type
    uopNext(i).src2Type := decodeUop.src2Type

    uopNext(i).src1Ready := renamingTable.io.OUT_renameReadReady(i)(0)
    uopNext(i).src2Ready := renamingTable.io.OUT_renameReadReady(i)(1)

    uopNext(i).fuType := decodeUop.fuType
    uopNext(i).opcode := decodeUop.opcode

    uopNext(i).imm := decodeUop.imm
    uopNext(i).pc := decodeUop.pc    

    uopNext(i).predTarget := decodeUop.predTarget
    uopNext(i).compressed := decodeUop.compressed

    uopNext(i).robPtr := robHeadPtr
    uopNext(i).ldqIndex := 0.U
    uopNext(i).stqIndex := 0.U

    uopNext(i).inst := decodeUop.inst
    uopNext(i).rs1 := decodeUop.rs1
    uopNext(i).rs2 := decodeUop.rs2
  }

  // * Control
  val inValid = io.IN_decodeUop.map(_.valid)
  val inFire = io.IN_decodeUop.map(_.fire)
  val outRobReady = io.IN_robReady  
  val outIssueQueueReady = io.IN_issueQueueReady
  val inReady = (0 until ISSUE_WIDTH).map(i => {
      (!uopRobValid(i) || outRobReady) && (!uopIssueQueueValid(i) || outIssueQueueReady(i))
    }
  ).reduce(_ && _)

  // ** maintain current uop
  for (i <- 0 until MACHINE_WIDTH) {
    when (io.IN_writebackUop(i).valid && io.IN_writebackUop(i).bits.prd =/= ZERO) {
      for (j <- 0 until ISSUE_WIDTH) {
        when (uopReg(j).prs1 === io.IN_writebackUop(i).bits.prd) {
          uopReg(j).src1Ready := true.B
        }
        when (uopReg(j).prs2 === io.IN_writebackUop(i).bits.prd) {
          uopReg(j).src2Ready := true.B
        }
      }
    }
  }
  // ** update uop
  for (i <- 0 until ISSUE_WIDTH) {
    uopReg(i) := Mux(inFire(i), uopNext(i), uopReg(i))
  }

  val issueQueueValid = VecInit((0 until ISSUE_WIDTH).map(i => 
    inValid(i) && uopNext(i).fuType =/= FuType.FLAG))

  // ** update uopValid
  when (io.IN_flush) {
    uopRobValid := VecInit(Seq.fill(ISSUE_WIDTH)(false.B))
    uopIssueQueueValid := VecInit(Seq.fill(ISSUE_WIDTH)(false.B))
  }.elsewhen(inReady) {
    uopRobValid := issueQueueValid
    uopIssueQueueValid := inValid
  }.otherwise {
    when(io.IN_robReady) {
      uopRobValid := VecInit(Seq.fill(ISSUE_WIDTH)(false.B))
    }
    for (i <- 0 until ISSUE_WIDTH) {
      when(io.IN_issueQueueReady(i)) {
        uopIssueQueueValid(i) := false.B
      }
    }    
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
    io.OUT_robValid(i) := uopRobValid(i)
    io.OUT_issueQueueValid(i) := uopIssueQueueValid(i)
    io.OUT_renameUop(i) := uopReg(i)
  }
}
