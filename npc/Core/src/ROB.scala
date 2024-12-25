import chisel3._
import chisel3.util._
import utils._

class ROBIO extends CoreBundle {
  val IN_renameUop = Flipped(Vec(ISSUE_WIDTH, Valid(new RenameUop)))
  val OUT_renameUopReady = Bool()
  val IN_writebackUop = Flipped(Vec(MACHINE_WIDTH, Valid(new WritebackUop)))
  val OUT_commitUop = Vec(COMMIT_WIDTH, Valid(new CommitUop))  
  val OUT_robTailPtr = Output(RingBufferPtr(ROB_SIZE))
  val IN_renameRobHeadPtr = Input(RingBufferPtr(ROB_SIZE))
  val OUT_redirect = Output(new RedirectSignal)

  val IN_flush = Input(Bool())
}

class ROBEntry extends CoreBundle {
  val rd   = UInt(5.W)
  val prd  = UInt(PREG_IDX_W)
  val flag = UInt(FLAG_W)
  val executed = Bool()

  // * temporary
  val target = UInt(XLEN.W)
}

class ROB extends CoreModule {
  val io = IO(new ROBIO)
  
  val rob = Reg(Vec(ROB_SIZE, new ROBEntry))
  val robStall = RegInit(false.B)

  // ** head/tail
  val robHeadPtr = RegInit(RingBufferPtr(size = ROB_SIZE, flag = 0.U, index = 0.U))  
  val robTailPtr = RegInit(RingBufferPtr(size = ROB_SIZE, flag = 1.U, index = 0.U))

  // ** Control
  val enqStall = io.IN_renameRobHeadPtr.isAheadOf(robTailPtr)
  for (i <- 0 until ISSUE_WIDTH) {
    io.OUT_renameUopReady := !enqStall
  }

  // ** enqueue
  for (i <- 0 until ISSUE_WIDTH) {
    val enqEntry = Wire(new ROBEntry)
    enqEntry.rd := io.IN_renameUop(i).bits.rd
    enqEntry.prd := io.IN_renameUop(i).bits.prd
    enqEntry.flag := io.IN_renameUop(i).bits.flag
    enqEntry.executed := false.B
    enqEntry.target := 0.U
    
    when (io.IN_renameUop(i).fire) {
      rob(io.IN_renameUop(i).bits.robPtr.index) := enqEntry
    }
  }

  // ** dequeue (Commit)
  val commitUop = Reg(Vec(COMMIT_WIDTH, new CommitUop))
  val commitValid = RegInit(VecInit(Seq.fill(COMMIT_WIDTH)(false.B)))
  val redirect = RegInit(0.U.asTypeOf(new RedirectSignal))

  val deqEntry = Wire(Vec(COMMIT_WIDTH, new ROBEntry))
  val deqValid = Wire(Vec(COMMIT_WIDTH, Bool()))

  for (i <- 0 until COMMIT_WIDTH) {
    val deqPtr = robTailPtr + i.U
    deqEntry(i) := rob(deqPtr.index)
    deqValid(i) := robHeadPtr.distanceTo(deqPtr) < ROB_SIZE.U && deqEntry(i).executed
  }

  commitValid := deqValid
  when(io.IN_flush || robStall) {
    commitValid := VecInit(Seq.fill(COMMIT_WIDTH)(false.B))
  }

  for (i <- 0 until COMMIT_WIDTH) {   
    commitUop(i).rd := deqEntry(i).rd
    commitUop(i).prd := deqEntry(i).prd
    commitUop(i).robPtr := robTailPtr + i.U
    redirect.valid := deqEntry(i).flag === Flags.MISPREDICT
    redirect.pc := deqEntry(i).target    
  }
  
  robStall := redirect.valid

  when(io.IN_flush) {
    robHeadPtr := RingBufferPtr(size = ROB_SIZE, flag = 0.U, index = 0.U)
    robTailPtr := RingBufferPtr(size = ROB_SIZE, flag = 1.U, index = 0.U)
  }.elsewhen(!robStall) {
    robHeadPtr := io.IN_renameRobHeadPtr
    robTailPtr := robTailPtr + PopCount(deqValid)
  }

  // val redirect = deqEntry.flag === Flags.MISPREDICT
  io.OUT_redirect := redirect

  // ** writeback
  for (i <- 0 until MACHINE_WIDTH) {
    val wbPtr = io.IN_writebackUop(i).bits.robPtr
    val wbEntry = rob(wbPtr.index)
    when (io.IN_writebackUop(i).valid) {
      wbEntry.executed := true.B
      wbEntry.target := io.IN_writebackUop(i).bits.target
      wbEntry.flag := io.IN_writebackUop(i).bits.flag
    }
  }

  io.OUT_robTailPtr := robTailPtr
  for (i <- 0 until COMMIT_WIDTH) {
    io.OUT_commitUop(i).valid := commitValid(i)
    io.OUT_commitUop(i).bits := commitUop(i)
  }
}
