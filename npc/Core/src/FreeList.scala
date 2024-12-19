import chisel3._
import chisel3.util._
import utils._

class FreeListIO extends CoreBundle {
  // * Allocate new PReg
  val IN_renameValid = Flipped(Vec(ISSUE_WIDTH, Bool()))
  val OUT_renamePReg = Vec(ISSUE_WIDTH, UInt(PREG_IDX_W))

  // * Free PReg (Commit)
  val IN_commitValid = Flipped(Vec(COMMIT_WIDTH, Bool()))
  val IN_commitPReg = Flipped(Vec(COMMIT_WIDTH, UInt(PREG_IDX_W)))
  val IN_commitPrevPReg = Flipped(Vec(COMMIT_WIDTH, UInt(PREG_IDX_W)))
  val IN_commitLastest = Flipped(Vec(COMMIT_WIDTH, Bool()))

  // * Reset
  val IN_flush = Input(Bool())
}

class FreeList extends CoreModule {
  val io = IO(new FreeListIO)
  val freeList = RegInit(VecInit((1 until NUM_PREG).map(_.U(PREG_IDX_W))))
  val headPtr = RegInit(RingBufferPtr(NUM_PREG - 1, 0.U, 0.U))
  val tailPtr = RegInit(RingBufferPtr(NUM_PREG - 1, 0.U, 0.U))

  // * Allocate new PReg
  for (i <- 0 until ISSUE_WIDTH) {
    when (io.IN_renameValid(i)) {
      io.OUT_renamePReg(i) := freeList(headPtr.index)
      headPtr := headPtr + 1.U
    } .otherwise {
      io.OUT_renamePReg(i) := ZERO
    }
  }

  // * Free PReg (Commit)
  for (i <- 0 until COMMIT_WIDTH) {
    when (io.IN_commitValid(i)) {
      freeList(tailPtr.index) := io.IN_commitPReg(i)
      tailPtr := tailPtr + 1.U
    }
  }
}
