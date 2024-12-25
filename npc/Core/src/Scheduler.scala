import chisel3._
import chisel3.util._
import utils._

class SchedulerIO extends CoreBundle {
  val IN_renameUop = Flipped(Vec(ISSUE_WIDTH, new RenameUop))
  val IN_issueQueueValid = Flipped(Vec(ISSUE_WIDTH, Bool()))
  val OUT_issueQueueReady = Vec(ISSUE_WIDTH, Bool())  
  val OUT_renameUop = Vec(MACHINE_WIDTH, Decoupled(new RenameUop))
}

class Scheduler extends CoreModule {
  val io = IO(new SchedulerIO)

  for (i <- 0 until ISSUE_WIDTH) {
    io.OUT_issueQueueReady(i) := false.B
  }

  for (i <- 0 until MACHINE_WIDTH) {
    io.OUT_renameUop(i).valid := false.B
    io.OUT_renameUop(i).bits := io.IN_renameUop(0)
  }

  def isALU(uop: RenameUop) = uop.fuType === FuType.ALU || uop.fuType === FuType.BRU
  def isLSU(uop: RenameUop) = uop.fuType === FuType.LSU
  // * ALU
  val aluValid = (0 until ISSUE_WIDTH).map(i => io.IN_issueQueueValid(i) && isALU(io.IN_renameUop(i)))
  val aluValidIndex = PriorityEncoder(aluValid)
  val hasAluValid = io.IN_issueQueueValid.reduce(_ || _)

  io.OUT_issueQueueReady(aluValidIndex) := io.OUT_renameUop(0).ready
  io.OUT_renameUop(0).valid := io.IN_issueQueueValid(aluValidIndex) && hasAluValid
  io.OUT_renameUop(0).bits := io.IN_renameUop(aluValidIndex)

  // * LSU
  val lsuValid = (0 until ISSUE_WIDTH).map(i => io.IN_issueQueueValid(i) && isLSU(io.IN_renameUop(i)))
  val lsuValidIndex = PriorityEncoder(lsuValid)
  val hasLsuValid = io.IN_issueQueueValid.reduce(_ || _)

  io.OUT_issueQueueReady(lsuValidIndex) := io.OUT_renameUop(1).ready
  io.OUT_renameUop(1).valid := io.IN_issueQueueValid(lsuValidIndex) && hasLsuValid
  io.OUT_renameUop(1).bits := io.IN_renameUop(lsuValidIndex)
}