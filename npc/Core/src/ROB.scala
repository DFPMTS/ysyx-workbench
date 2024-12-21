import chisel3._
import chisel3.util._
import utils._

class ROBIO extends CoreBundle {
  val IN_renameUop = Flipped(Vec(ISSUE_WIDTH, Decoupled(new RenameUop)))
  val IN_writebackUop = Flipped(Vec(MACHINE_WIDTH, Valid(new WritebackUop)))
  val OUT_commitUop = Vec(COMMIT_WIDTH, Valid(new CommitUop))  
  val OUT_robTailPtr = Output(RingBufferPtr(ROB_SIZE))
  val IN_renameRobHeadPtr = Input(RingBufferPtr(ROB_SIZE))
}

class ROBEntry extends CoreBundle {
  val rd   = UInt(5.W)
  val prd  = UInt(PREG_IDX_W)
  val flag = UInt(FLAG_W)
  val executed = Bool()
}

class ROB extends CoreModule {
  val io = IO(new ROBIO)
  
  val rob = Reg(Vec(ROB_SIZE, new ROBEntry))

  // ** head/tail
  val robHeadPtr = RegInit(RingBufferPtr(size = ROB_SIZE, flag = 0.U, index = 0.U))
  robHeadPtr := io.IN_renameRobHeadPtr
  val robTailPtr = RegInit(RingBufferPtr(size = ROB_SIZE, flag = 1.U, index = 0.U))

  // ** Control
  val enqStall = io.IN_renameRobHeadPtr.isAheadOf(robTailPtr)
  for (i <- 0 until ISSUE_WIDTH) {
    io.IN_renameUop(i).ready := !enqStall
  }

  // ** enqueue
  for (i <- 0 until ISSUE_WIDTH) {
    val enqEntry = Wire(new ROBEntry)
    enqEntry.rd := io.IN_renameUop(i).bits.rd
    enqEntry.prd := io.IN_renameUop(i).bits.prd
    enqEntry.flag := io.IN_renameUop(i).bits.flag

    val enqPtr = robTailPtr + i.U
    when (io.IN_renameUop(i).fire) {
      rob(enqPtr.index) := enqEntry
    }
  }

  // ** dequeue (Commit)
  for (i <- 0 until COMMIT_WIDTH) {
    val deqPtr = robTailPtr + i.U
    val deqEntry = rob(deqPtr.index)
    val deqValid = robHeadPtr.distanceTo(deqPtr) < ROB_SIZE.U && deqEntry.executed

    io.OUT_commitUop(i).valid := deqValid
    io.OUT_commitUop(i).bits.rd := deqEntry.rd
    io.OUT_commitUop(i).bits.prd := deqEntry.prd    
  }

  // ** writeback
  for (i <- 0 until MACHINE_WIDTH) {
    val wbPtr = io.IN_writebackUop(i).bits.robPtr
    val wbEntry = rob(wbPtr.index)
    when (io.IN_writebackUop(i).valid) {
      wbEntry.executed := true.B
    }
  }
}
