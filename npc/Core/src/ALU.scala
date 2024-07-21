import chisel3._
import chisel3.util._

trait HasALUFuncs {
  def ALU_X = BitPat("b????")

  def ALU_ADD = "b0000".U(4.W)
  def ALU_SUB = "b0001".U(4.W)

  def ALU_LEFT  = "b0010".U(4.W)
  def ALU_RIGHT = "b0011".U(4.W)

  def ALU_EQ = "b0100".U(4.W)
  def ALU_NE = "b0101".U(4.W)

  // "b0110"
  // "b0111"

  def ALU_AND   = "b1000".U(4.W)
  def ALU_OR    = "b1001".U(4.W)
  def ALU_XOR   = "b1010".U(4.W)
  def ALU_ARITH = "b1011".U(4.W)

  def ALU_LT  = "b1100".U(4.W)
  def ALU_LTU = "b1101".U(4.W)

  def ALU_GE  = "b1110".U(4.W)
  def ALU_GEU = "b1111".U(4.W)
}

class ALU extends Module with HasALUFuncs {
  val io = IO(new Bundle {
    val op1     = Input(UInt(32.W))
    val op2     = Input(UInt(32.W))
    val aluFunc = Input(UInt(4.W))
    val out     = Output(UInt(32.W))
    val cmpOut  = Output(Bool())
  })

  // [adder] add / sub
  val isSub    = ~(io.aluFunc === ALU_ADD) // for cmp
  val op2Adder = Mux(isSub, ~io.op2, io.op2)
  val addRes   = io.op1 + op2Adder + isSub

  // [shift] left / right / arith
  val shamt = io.op2(4, 0)
  // [logic] and / or / xor
  val xorRes = io.op1 ^ io.op2
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
  val op1MSB = io.op1(31);
  val op2MSB = io.op2(31);
  val subMSB = addRes(31);
  val ltRes  = Mux(op1MSB === op2MSB, subMSB, Mux(io.aluFunc(0), op2MSB, op1MSB))
  val geRes  = ~ltRes

  io.out := MuxLookup(io.aluFunc, addRes)(
    Seq(
      ALU_LEFT -> (io.op1 << shamt),
      ALU_RIGHT -> (io.op1 >> shamt),
      ALU_EQ -> eqRes,
      ALU_NE -> neRes,
      ALU_AND -> (io.op1 & io.op2),
      ALU_OR -> (io.op1 | io.op2),
      ALU_XOR -> xorRes,
      ALU_ARITH -> (io.op1.asSInt >> shamt).asUInt,
      ALU_LT -> ltRes,
      ALU_LTU -> ltRes,
      ALU_GE -> geRes,
      ALU_GEU -> geRes
    )
  )
  io.cmpOut := io.out(0)
}
