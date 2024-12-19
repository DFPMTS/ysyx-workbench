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

  val uop = Reg(Vec(ISSUE_WIDTH, new RenameUop))
  val uopNext = Wire(Vec(ISSUE_WIDTH, new RenameUop()))
  val uopValid = RegInit(Vec(ISSUE_WIDTH, Bool()))
  
  // * Submodules
  val renamingTable = new RenamingTable
  val freeList = new FreeList

  // * Decode -> FreeList
  for (i <- 0 until ISSUE_WIDTH) {
    freeList.io.IN_renameReqValid(i) := io.IN_decodeUop(i).valid && io.IN_decodeUop(i).bits.rd =/= 0.U
  }
  val renameStall = freeList.io.OUT_renameStall

  // * Decode -> RenamingTable
  for (i <- 0 until ISSUE_WIDTH) {
    renamingTable.io.IN_renameReadAReg(i) := VecInit(io.IN_decodeUop(i).bits.rs1, io.IN_decodeUop(i).bits.rs2)
    renamingTable.io.IN_renameWriteValid(i) := !renameStall && io.IN_decodeUop(i).valid && io.IN_decodeUop(i).bits.rd =/= 0.U
    renamingTable.io.IN_renameWriteAReg(i) := io.IN_decodeUop(i).bits.rd
    renamingTable.io.IN_renameWritePReg(i) := freeList.io.OUT_renamePReg(i)
  }

  // * RenamingTable <- Commit
  for (i <- 0 until MACHINE_WIDTH) {
    renamingTable.io.IN_writebackValid(i) := io.IN_writebackUop(i).valid
    renamingTable.io.IN_writebackPReg(i) := io.IN_writebackUop(i).bits.prd
  }

  // * RenamingTable <- Commit
  for (i <- 0 until COMMIT_WIDTH) {
    renamingTable.io.IN_commitValid(i) := io.IN_commitUop(i).valid
    renamingTable.io.IN_commitAReg(i) := io.IN_commitUop(i).bits.rd
    renamingTable.io.IN_commitPReg(i) := io.IN_commitUop(i).bits.prd
  }

  // * Output Logic
  // ** Decode <- Rename
  for (i <- 0 until ISSUE_WIDTH) {
    io.IN_decodeUop(i).ready := !renameStall
  }

}
