import chisel3._
import chisel3.util._
import chisel3.util.experimental._
import chisel3.util.experimental.decode.TruthTable
import chisel3.util.experimental.decode.decoder
import scala.language.implicitConversions
import utils._

class DecodeSignal extends Bundle with HasDecodeConfig{
  val invalid  = Bool()
  val regWe    = Bool()
  val src1Type = UInt(2.W)
  val src2Type = UInt(2.W)  
  val fuType   = UInt(FuTypeWidth.W)
  val opcode   = UInt(OpcodeWidth.W)
  val immType = UInt(ImmTypeWidth.W)
}

class Decode extends CoreModule {
  val io = IO(new Bundle {
    val inst    = Input(UInt(32.W))
    val signals = Output(new DecodeSignal)
  })
  implicit def uintToBitPat(x: UInt): BitPat = BitPat(x)
  def lui        = BitPat("b??????? ????? ????? ??? ????? 01101 11")
  def auipc      = BitPat("b??????? ????? ????? ??? ????? 00101 11")
  def jal        = BitPat("b??????? ????? ????? ??? ????? 11011 11")
  def jalr       = BitPat("b??????? ????? ????? ??? ????? 11001 11")
  def beq        = BitPat("b??????? ????? ????? 000 ????? 11000 11")
  def bne        = BitPat("b??????? ????? ????? 001 ????? 11000 11")
  def blt        = BitPat("b??????? ????? ????? 100 ????? 11000 11")
  def bge        = BitPat("b??????? ????? ????? 101 ????? 11000 11")
  def bltu       = BitPat("b??????? ????? ????? 110 ????? 11000 11")
  def bgeu       = BitPat("b??????? ????? ????? 111 ????? 11000 11")
  def lb         = BitPat("b??????? ????? ????? 000 ????? 00000 11")
  def lh         = BitPat("b??????? ????? ????? 001 ????? 00000 11")
  def lw         = BitPat("b??????? ????? ????? 010 ????? 00000 11")
  def lbu        = BitPat("b??????? ????? ????? 100 ????? 00000 11")
  def lhu        = BitPat("b??????? ????? ????? 101 ????? 00000 11")
  def sb         = BitPat("b??????? ????? ????? 000 ????? 01000 11")
  def sh         = BitPat("b??????? ????? ????? 001 ????? 01000 11")
  def sw         = BitPat("b??????? ????? ????? 010 ????? 01000 11")
  def addi       = BitPat("b??????? ????? ????? 000 ????? 00100 11")
  def slti       = BitPat("b??????? ????? ????? 010 ????? 00100 11")
  def sltiu      = BitPat("b??????? ????? ????? 011 ????? 00100 11")
  def xori       = BitPat("b??????? ????? ????? 100 ????? 00100 11")
  def ori        = BitPat("b??????? ????? ????? 110 ????? 00100 11")
  def andi       = BitPat("b??????? ????? ????? 111 ????? 00100 11")
  def slli       = BitPat("b0000000 ????? ????? 001 ????? 00100 11")
  def srli       = BitPat("b0000000 ????? ????? 101 ????? 00100 11")
  def srai       = BitPat("b0100000 ????? ????? 101 ????? 00100 11")
  def add        = BitPat("b0000000 ????? ????? 000 ????? 01100 11")
  def sub        = BitPat("b0100000 ????? ????? 000 ????? 01100 11")
  def sll        = BitPat("b0000000 ????? ????? 001 ????? 01100 11")
  def slt        = BitPat("b0000000 ????? ????? 010 ????? 01100 11")
  def sltu       = BitPat("b0000000 ????? ????? 011 ????? 01100 11")
  def xor        = BitPat("b0000000 ????? ????? 100 ????? 01100 11")
  def srl        = BitPat("b0000000 ????? ????? 101 ????? 01100 11")
  def sra        = BitPat("b0100000 ????? ????? 101 ????? 01100 11")
  def or         = BitPat("b0000000 ????? ????? 110 ????? 01100 11")
  def and        = BitPat("b0000000 ????? ????? 111 ????? 01100 11")
  def fence_i    = BitPat("b??????? ????? ????? 001 ????? 00011 11")
  def ecall      = BitPat("b0000000 00000 00000 000 00000 11100 11")
  def ebreak     = BitPat("b0000000 00001 00000 000 00000 11100 11")
  def csrrw      = BitPat("b??????? ????? ????? 001 ????? 11100 11")
  def csrrs      = BitPat("b??????? ????? ????? 010 ????? 11100 11")
  def mret       = BitPat("b0011000 00010 00000 000 00000 11100 11")


  val defaultCtrl: List[BitPat] = List(Y, N, BitPat.dontCare(2), BitPat.dontCare(2), BitPat.dontCare(FuTypeWidth), BitPat.dontCare(OpcodeWidth), ImmType.X)
  val lut: List[(BitPat, List[BitPat])] = List(
lui        -> List(N, Y, SrcType.ZERO, SrcType.IMM,  FuType.ALU, ALUOp.ADD,       ImmType.U),
auipc      -> List(N, Y, SrcType.PC,   SrcType.IMM,  FuType.ALU, ALUOp.ADD,       ImmType.U),
jal        -> List(N, Y, SrcType.PC,   SrcType.IMM,  FuType.BRU, BRUOp.JAL,       ImmType.J),
jalr       -> List(N, Y, SrcType.REG,  SrcType.IMM,  FuType.BRU, BRUOp.JALR,      ImmType.I),
beq        -> List(N, N, SrcType.REG,  SrcType.REG,  FuType.BRU, BRUOp.BEQ,       ImmType.B),
bne        -> List(N, N, SrcType.REG,  SrcType.REG,  FuType.BRU, BRUOp.BNE,       ImmType.B),
blt        -> List(N, N, SrcType.REG,  SrcType.REG,  FuType.BRU, BRUOp.BLT,       ImmType.B),
bge        -> List(N, N, SrcType.REG,  SrcType.REG,  FuType.BRU, BRUOp.BGE,       ImmType.B),
bltu       -> List(N, N, SrcType.REG,  SrcType.REG,  FuType.BRU, BRUOp.BLTU,      ImmType.B),
bgeu       -> List(N, N, SrcType.REG,  SrcType.REG,  FuType.BRU, BRUOp.BGEU,      ImmType.B),
lb         -> List(N, Y, SrcType.REG,  SrcType.IMM,  FuType.AGU, LSUOp.LB,        ImmType.I),
lh         -> List(N, Y, SrcType.REG,  SrcType.IMM,  FuType.AGU, LSUOp.LH,        ImmType.I),
lw         -> List(N, Y, SrcType.REG,  SrcType.IMM,  FuType.AGU, LSUOp.LW,        ImmType.I),
lbu        -> List(N, Y, SrcType.REG,  SrcType.IMM,  FuType.AGU, LSUOp.LBU,       ImmType.I),
lhu        -> List(N, Y, SrcType.REG,  SrcType.IMM,  FuType.AGU, LSUOp.LHU,       ImmType.I),
sb         -> List(N, N, SrcType.REG,  SrcType.REG,  FuType.AGU, LSUOp.SB,        ImmType.S),
sh         -> List(N, N, SrcType.REG,  SrcType.REG,  FuType.AGU, LSUOp.SH,        ImmType.S),
sw         -> List(N, N, SrcType.REG,  SrcType.REG,  FuType.AGU, LSUOp.SW,        ImmType.S),
addi       -> List(N, Y, SrcType.REG,  SrcType.IMM,  FuType.ALU, ALUOp.ADD,       ImmType.I),
slti       -> List(N, Y, SrcType.REG,  SrcType.IMM,  FuType.ALU, ALUOp.LT,        ImmType.I),
sltiu      -> List(N, Y, SrcType.REG,  SrcType.IMM,  FuType.ALU, ALUOp.LTU,       ImmType.I),
xori       -> List(N, Y, SrcType.REG,  SrcType.IMM,  FuType.ALU, ALUOp.XOR,       ImmType.I),
ori        -> List(N, Y, SrcType.REG,  SrcType.IMM,  FuType.ALU, ALUOp.OR,        ImmType.I),
andi       -> List(N, Y, SrcType.REG,  SrcType.IMM,  FuType.ALU, ALUOp.AND,       ImmType.I),
slli       -> List(N, Y, SrcType.REG,  SrcType.IMM,  FuType.ALU, ALUOp.LEFT,      ImmType.I),
srli       -> List(N, Y, SrcType.REG,  SrcType.IMM,  FuType.ALU, ALUOp.RIGHT,     ImmType.I),
srai       -> List(N, Y, SrcType.REG,  SrcType.IMM,  FuType.ALU, ALUOp.ARITH,     ImmType.I),
add        -> List(N, Y, SrcType.REG,  SrcType.REG,  FuType.ALU, ALUOp.ADD,       ImmType.X),
sub        -> List(N, Y, SrcType.REG,  SrcType.REG,  FuType.ALU, ALUOp.SUB,       ImmType.X),
sll        -> List(N, Y, SrcType.REG,  SrcType.REG,  FuType.ALU, ALUOp.LEFT,      ImmType.X),
slt        -> List(N, Y, SrcType.REG,  SrcType.REG,  FuType.ALU, ALUOp.LT,        ImmType.X),
sltu       -> List(N, Y, SrcType.REG,  SrcType.REG,  FuType.ALU, ALUOp.LTU,       ImmType.X),
xor        -> List(N, Y, SrcType.REG,  SrcType.REG,  FuType.ALU, ALUOp.XOR,       ImmType.X),
srl        -> List(N, Y, SrcType.REG,  SrcType.REG,  FuType.ALU, ALUOp.RIGHT,     ImmType.X),
sra        -> List(N, Y, SrcType.REG,  SrcType.REG,  FuType.ALU, ALUOp.ARITH,     ImmType.X),
or         -> List(N, Y, SrcType.REG,  SrcType.REG,  FuType.ALU, ALUOp.OR,        ImmType.X),
and        -> List(N, Y, SrcType.REG,  SrcType.REG,  FuType.ALU, ALUOp.AND,       ImmType.X),
// fence_i    -> List(N, N, ZERO, IMM,  FuType.CSR, CSROp.FENCE_I,   ImmType.X),
ecall      -> List(N, N, SrcType.ZERO, SrcType.ZERO, FuType.CSR, CSROp.ECALL,     ImmType.X),
ebreak     -> List(N, N, SrcType.ZERO, SrcType.ZERO, FuType.CSR, CSROp.EBREAK,    ImmType.X),
csrrw      -> List(N, Y, SrcType.REG,  SrcType.ZERO, FuType.CSR, CSROp.CSRRW,     ImmType.I),
csrrs      -> List(N, Y, SrcType.REG,  SrcType.ZERO, FuType.CSR, CSROp.CSRRS,     ImmType.I),
mret       -> List(N, N, SrcType.ZERO, SrcType.ZERO, FuType.CSR, CSROp.MRET,      ImmType.X),

  )
  def listToBitPat(l: List[BitPat]) = {
    l.reduceLeft(_ ## _)
  }

  def transformLUT(lut: List[(BitPat, List[BitPat])]): List[(BitPat, BitPat)] = {
    lut.map {
      case (key, value) =>
        (key, listToBitPat(value))
    }
  }

  val table = TruthTable(transformLUT(lut), listToBitPat(defaultCtrl))
  io.signals := decoder(io.inst, table).asTypeOf(new DecodeSignal)
}
