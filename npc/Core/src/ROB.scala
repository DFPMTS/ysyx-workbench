import chisel3._
import chisel3.util._
import utils._

class ROBIO extends CoreBundle {
  val IN_renameUop = Flipped(Vec(ISSUE_WIDTH, Decoupled(new RenameUop)))
  val IN_writebackUop = Flipped(Vec(MACHINE_WIDTH, Valid(new WritebackUop)))
  val OUT_commitUop = Vec(COMMIT_WIDTH, Valid(new CommitUop))  
}

class ROBEntry extends CoreBundle {
  val rd   = UInt(5.W)
  val prd  = UInt(PREG_IDX_W)
  val flag = UInt(FLAG_W)
}

class ROB extends CoreModule {
  val io = IO(new ROBIO)
    
  val headPtr = RegInit(RingBufferPtr(size = ROB_SIZE, flag = 0.U, index = 0.U))
  val tailPtr = RegInit(RingBufferPtr(size = ROB_SIZE, flag = 1.U, index = 0.U))

  val rob = Reg(Vec(ROB_SIZE, new ROBEntry))

  val inSlotReady = headPtr.distanceTo(tailPtr) < ISSUE_WIDTH.U
  for (i <- 0 until ISSUE_WIDTH) {
    io.IN_renameUop(i).ready := inSlotReady
  }

  for (i <- 0 until ISSUE_WIDTH) {
    val enqEntry = Wire(new ROBEntry)
    enqEntry.rd := io.IN_renameUop(i).bits.rd
    enqEntry.prd := io.IN_renameUop(i).bits.prd
    enqEntry.flag := io.IN_renameUop(i).bits.flag

    val enqPtr = headPtr + i.U
  }


}
