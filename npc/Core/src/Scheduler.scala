import chisel3._
import chisel3.util._
import utils._

class SchedulerIO extends CoreBundle {
  val IN_renameUop = Flipped(Vec(ISSUE_WIDTH, new RenameUop))  
  val IN_issueQueueValid = Flipped(Vec(MACHINE_WIDTH, Bool()))
  val OUT_issueQueueReady = Output(Vec(MACHINE_WIDTH, Bool()))
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

  val validIndex = PriorityEncoder(io.IN_issueQueueValid)
  val hasValid = io.IN_issueQueueValid.reduce(_ || _)

  io.OUT_issueQueueReady(validIndex) := io.OUT_renameUop(0).ready
  io.OUT_renameUop(0).valid := io.IN_issueQueueValid(validIndex)
  io.OUT_renameUop(0).bits := io.IN_renameUop(validIndex)
}