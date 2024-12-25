import chisel3._
import chisel3.util._
import utils._

class IssueQueueIO extends CoreBundle {
  val IN_renameUop = Flipped(Decoupled(new RenameUop))
  val IN_writebackUop = Flipped(Vec(MACHINE_WIDTH, Valid(new WritebackUop)))
  val OUT_issueUop = Decoupled(new RenameUop)
  val IN_robTailPtr = Input(RingBufferPtr(ROB_SIZE))
  val IN_flush = Input(Bool())
}

class IssueQueue(FUs: Seq[UInt]) extends CoreModule {
  val io = IO(new IssueQueueIO)

  def hasFU(fu: UInt) = {
    FUs.contains(fu)
  }

  def isFuType(uop: RenameUop, fu: UInt) = {
    uop.fuType === fu
  }

  // ** Dequeue Uop
  val uopNext = Wire(new RenameUop)
  val uop = Reg(new RenameUop)
  val uopValid = RegInit(false.B)

  val queue = Reg(Vec(IQ_SIZE, new RenameUop))

  val headIndex = RegInit(0.U(IQ_IDX_W + 1))

  // ** Writeback
  val writebackReady = Wire(Vec(IQ_SIZE, Vec(2, Bool())))
  for (j <- 0 until IQ_SIZE) {
    writebackReady(j)(0) := false.B
    writebackReady(j)(1) := false.B
    for (i <- 0 until MACHINE_WIDTH) {
      val writebackValid = io.IN_writebackUop(i).valid
      val writebackUop = io.IN_writebackUop(i).bits
      when (writebackValid) {
        when (queue(j).prs1 === writebackUop.prd) {
          queue(j).src1Ready := true.B
          writebackReady(j)(0) := true.B
        }
        when (queue(j).prs2 === writebackUop.prd) {
          queue(j).src2Ready := true.B
          writebackReady(j)(1) := true.B
        }
      }
    }    
  }  

  val readyVec = (0 until IQ_SIZE).map(i => {
    (i.U < headIndex && (queue(i).src1Ready || writebackReady(i)(0)) && 
                        (queue(i).src2Ready || writebackReady(i)(1))) &&
    (!hasFU(FuType.LSU).B || queue(i).fuType =/= FuType.LSU || queue(i).robPtr.index === io.IN_robTailPtr.index)
  })
  
  val hasReady = readyVec.reduce(_ || _)
  val deqIndex = PriorityEncoder(readyVec)
 
  val updateValid = io.OUT_issueUop.fire || !uopValid

  val doEnq = io.IN_renameUop.fire
  val doDeq = updateValid && hasReady

  val enqStall = headIndex === IQ_SIZE.U
  io.IN_renameUop.ready := !enqStall

  when (io.IN_flush) {
    headIndex := 0.U
  }.otherwise {
    headIndex := headIndex + doEnq - doDeq  
  }

  // ** Update output Registers
  uopNext := queue(deqIndex)
  when (doDeq) {
    uop := uopNext
    for (i <- 0 until IQ_SIZE - 1) {
      when (i.U >= deqIndex) {
        queue(i) := queue(i + 1)
        queue(i).src1Ready := queue(i + 1).src1Ready || writebackReady(i + 1)(0)
        queue(i).src2Ready := queue(i + 1).src2Ready || writebackReady(i + 1)(1)
      }      
    }
  }
  when (doEnq) {
    val enqIndex = Mux(doDeq, headIndex - 1.U, headIndex)(IQ_IDX_W.get - 1, 0)
    val renameUop = io.IN_renameUop.bits
    queue(enqIndex) := renameUop
    // * fix srcReady
    for (i <- 0 until MACHINE_WIDTH) {
      val writebackValid = io.IN_writebackUop(i).valid
      val writebackUop = io.IN_writebackUop(i).bits
      when (writebackValid) {
        when (renameUop.prs1 === writebackUop.prd) {
          queue(enqIndex).src1Ready := true.B
        }
        when (renameUop.prs2 === writebackUop.prd) {
          queue(enqIndex).src2Ready := true.B
        }
      }
    }    
  }

  when (io.IN_flush) {
    uopValid := false.B
  }.elsewhen (updateValid) {
    uopValid := hasReady
  }

  // ** Output
  io.OUT_issueUop.valid := uopValid
  io.OUT_issueUop.bits := uop
}