import chisel3._
import chisel3.util._
import utils._

class RenameIO extends Bundle {
  // * rename
  val IN_decodeUop = Flipped(Decoupled(new DecodeUop))
  val OUT_renameUop = Decoupled(new RenameUop)
  // * writeback
  val IN_writebackUop = Flipped(Valid(new WritebackUop))
  // * commit
  val IN_commitUop = Flipped(Valid(new CommitUop))

  val IN_flush     = Input(Bool())
}

class Rename extends CoreModule {
  val io = IO(new RenameIO)

  // * Submodules
  val renamingTable = new RenamingTable
  val freeList = new FreeList
}
