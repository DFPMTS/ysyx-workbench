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

  def isPort0(uop: RenameUop) = uop.fuType === FuType.ALU || uop.fuType === FuType.BRU || uop.fuType === FuType.CSR
  def isPort1(uop: RenameUop) = uop.fuType === FuType.LSU
  // * Port 0: ALU/BRU/CSR
  val port0Valid = (0 until ISSUE_WIDTH).map(i => io.IN_issueQueueValid(i) && isPort0(io.IN_renameUop(i)))
  val port0ValidIndex = PriorityEncoder(port0Valid)
  val hasPort0Valid = port0Valid.reduce(_ || _)

  io.OUT_issueQueueReady(port0ValidIndex) := io.OUT_renameUop(0).ready
  io.OUT_renameUop(0).valid := io.IN_issueQueueValid(port0ValidIndex) && hasPort0Valid
  io.OUT_renameUop(0).bits := io.IN_renameUop(port0ValidIndex)

  // * Port 1: LSU
  val port1Valid = (0 until ISSUE_WIDTH).map(i => io.IN_issueQueueValid(i) && isPort1(io.IN_renameUop(i)))
  val port1ValidIndex = PriorityEncoder(port1Valid)
  val hasPort1Valid = port1Valid.reduce(_ || _)

  io.OUT_issueQueueReady(port1ValidIndex) := io.OUT_renameUop(1).ready
  io.OUT_renameUop(1).valid := io.IN_issueQueueValid(port1ValidIndex) && hasPort1Valid
  io.OUT_renameUop(1).bits := io.IN_renameUop(port1ValidIndex)
}