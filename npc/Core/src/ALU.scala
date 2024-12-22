import chisel3._
import chisel3.util._
import utils.CoreBundle

trait HasALUFuncs {
  def ALU_X = BitPat("b????")

  def ALU_ADD = "b0000".U(4.W)
  def ALU_SUB = "b0001".U(4.W)

  def ALU_LEFT  = "b0010".U(4.W)
  def ALU_RIGHT = "b0011".U(4.W)

  def ALU_AND   = "b0100".U(4.W)
  def ALU_OR    = "b0101".U(4.W)
  def ALU_XOR   = "b0110".U(4.W)
  def ALU_ARITH = "b0111".U(4.W)

  // "b1000"
  // "b1001"

  def ALU_EQ = "b1010".U(4.W)
  def ALU_NE = "b1011".U(4.W)

  def ALU_LT  = "b1100".U(4.W)
  def ALU_LTU = "b1101".U(4.W)

  def ALU_GE  = "b1110".U(4.W)
  def ALU_GEU = "b1111".U(4.W)
}

class ALUIO extends CoreBundle {
  val IN_readRegUop = Flipped(Decoupled(new ReadRegUop))
  val OUT_writebackUop = Valid(new WritebackUop)
}

class ALU extends Module with HasALUFuncs {
  val io = IO(new ALUIO)

  val op1        = io.IN_readRegUop.bits.src1
  val op2        = io.IN_readRegUop.bits.src2
  val imm        = io.IN_readRegUop.bits.imm
  val pc         = io.IN_readRegUop.bits.pc
  val fuType     = io.IN_readRegUop.bits.fuType
  val aluFunc    = io.IN_readRegUop.bits.opcode
  val out        = UInt(32.W)
  val cmpOut     = out(0)
  val jumpTarget = UInt(32.W)

  // [adder] add / sub
  val isSub    = ~(aluFunc === ALUOp.ADD || aluFunc === BRUOp.JAL || aluFunc === BRUOp.JALR) // for cmp
  val op2Adder = Mux(isSub, ~op2, op2)
  val addRes   = op1 + op2Adder + isSub

  // [shift] left / right / arith
  val shamt = op2(4, 0)
  // [logic] and / or / xor
  val xorRes = op1 ^ op2
  // [cmp] eq / ne / lt / ge
  val eqRes = xorRes === 0.U
  val neRes = ~eqRes
  /*                 lt
     op1MSB  | op2MSB  |   U   |   S
        0         0     subMSB  subMSB
        1         1     subMSB  subMSB
        0         1        1       0
        1         0        0       1
   */
  val op1MSB = op1(31);
  val op2MSB = op2(31);
  val subMSB = addRes(31);
  val ltRes  = Mux(op1MSB === op2MSB, subMSB, Mux(aluFunc(0), op2MSB, op1MSB))
  val geRes  = ~ltRes

  // ** ALU's default out should be add
  out := MuxLookup(aluFunc, addRes)(
    Seq(
      ALUOp.LEFT -> (op1 << shamt),
      ALUOp.RIGHT -> (op1 >> shamt),
      ALUOp.EQ -> eqRes,
      ALUOp.NE -> neRes,
      ALUOp.AND -> (op1 & op2),
      ALUOp.OR -> (op1 | op2),
      ALUOp.XOR -> xorRes,
      ALUOp.ARITH -> (op1.asSInt >> shamt).asUInt,
      ALUOp.LT -> ltRes,
      ALUOp.LTU -> ltRes,
      ALUOp.GE -> geRes,
      ALUOp.GEU -> geRes
    )
  )  

  val aluUop = Wire(new WritebackUop)
  aluUop.data := out
  aluUop.prd := io.IN_readRegUop.bits.prd
  aluUop.robPtr := io.IN_readRegUop.bits.robPtr
  aluUop.flag := 0.U
  aluUop.target := 0.U

  // ** BRU
  // ** Branch's result is calculated in ALU (Branch Opcode is same as corresponding ALU Opcode)
  // ** BRUOp.JAL and BRUOp.JALR's target are also calculated in ALU  
  // ** AUIPC's result is calculated in ALU
  val isBRU = fuType === FuType.BRU
  val isJump = aluFunc === BRUOp.JAL || aluFunc === BRUOp.JALR
  val isBranch = aluFunc === BRUOp.BEQ || aluFunc === BRUOp.BNE || aluFunc === BRUOp.BLT || aluFunc === BRUOp.BGE || aluFunc === BRUOp.BLTU || aluFunc === BRUOp.BGEU

  val mispredict = jumpTarget =/= io.IN_readRegUop.bits.predTarget
  val branchJump = pc + imm
  val nextInstPC = pc + 4.U  
  val branchTarget = Mux(cmpOut, branchJump, nextInstPC)
  jumpTarget := Mux(isJump, branchTarget, out)

  val bruUop = Wire(new WritebackUop)
  bruUop.data := Mux(isJump, nextInstPC, out)
  bruUop.prd := io.IN_readRegUop.bits.prd
  bruUop.robPtr := io.IN_readRegUop.bits.robPtr
  bruUop.flag := mispredict

  val uop = Reg(new WritebackUop)
  val uopValid = RegInit(false.B)

  io.IN_readRegUop.ready := true.B

  uopValid := io.IN_readRegUop.valid

  uop := Mux(isBRU, bruUop, aluUop)

  // ** Output
  io.OUT_writebackUop.valid := uopValid
  io.OUT_writebackUop := uop
}

// class testALU extends Module {
//   val io = IO(
//     new Bundle {
//       val t0 = Input(UInt(32.W))
//       val s0 = Output(UInt(32.W))
//       val s1 = Output(UInt(1.W))
//     }
//   )
//   val x0   = Reg(UInt(32.W))
//   val func = Reg(UInt(4.W))
//   val s0   = Reg(UInt(32.W))
//   val s1   = Reg(UInt(1.W))
//   dontTouch(x0)
//   val alu = Module(new ALU)
//   alu.io.op1     := s0
//   alu.io.op2     := io.t0
//   alu.io.aluFunc := func
//   s0             := alu.io.out
//   s1             := alu.io.cmpOut
//   io.s0          := s0
//   io.s1          := s1
// }

class testALU extends Module {
  val io = IO(
    new Bundle {
      val t0 = Input(UInt(32.W))
      val t1 = Input(UInt(32.W))
      val s0 = Output(UInt(32.W))
    }
  )
  val s0 = RegInit(0.U(32.W))
  s0 := s0 + io.t1

  io.s0 := s0
}
