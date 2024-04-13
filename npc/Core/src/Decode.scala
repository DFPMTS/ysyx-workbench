import chisel3._
import chisel3.util._
import chisel3.util.experimental._
import chisel3.util.experimental.decode.TruthTable
import chisel3.util.experimental.decode.decoder

class Control extends Bundle {
  val invalid   = UInt(1.W)
  val ebreak    = UInt(1.W)
  val reg_we    = UInt(1.W)
  val alu_sel1  = UInt(2.W)
  val alu_sel2  = UInt(1.W)
  val cmp_U     = UInt(1.W)
  val alu_func  = UInt(4.W)
  val branch    = UInt(1.W)
  val jal       = UInt(1.W)
  val jalr      = UInt(1.W)
  val wb_sel    = UInt(2.W)
  val inst_type = UInt(3.W)
  val mr        = UInt(1.W)
  val mw        = UInt(1.W)
}

class Decode extends Module {
  val io = IO(new Bundle {
    val inst    = Input(UInt(32.W))
    val ctrl    = Output(new Control)
    val rd      = Output(UInt(5.W))
    val rs1     = Output(UInt(5.W))
    val rs2     = Output(UInt(5.W))
    val mem_len = Output(UInt(2.W))
    val load_U  = Output(Bool())
  })

  val lut = List(
    BitPat("b??????? ????? ????? ??? ????? 01101 11") -> BitPat("b001101?00000000000100"),
    BitPat("b??????? ????? ????? ??? ????? 00101 11") -> BitPat("b001011?00000000000100"),
    BitPat("b??????? ????? ????? ??? ????? 11011 11") -> BitPat("b001011?00000101010000"),
    BitPat("b??????? ????? ????? ??? ????? 11001 11") -> BitPat("b001001?00000011000000"),
    BitPat("b??????? ????? ????? 000 ????? 11000 11") -> BitPat("b00000000101100??01100"),
    BitPat("b??????? ????? ????? 001 ????? 11000 11") -> BitPat("b00000000110100??01100"),
    BitPat("b??????? ????? ????? 100 ????? 11000 11") -> BitPat("b00000000111100??01100"),
    BitPat("b??????? ????? ????? 101 ????? 11000 11") -> BitPat("b00000001000100??01100"),
    BitPat("b??????? ????? ????? 110 ????? 11000 11") -> BitPat("b00000010111100??01100"),
    BitPat("b??????? ????? ????? 111 ????? 11000 11") -> BitPat("b00000011000100??01100"),
    BitPat("b??????? ????? ????? 000 ????? 00000 11") -> BitPat("b001001?00000000100010"),
    BitPat("b??????? ????? ????? 001 ????? 00000 11") -> BitPat("b001001?00000000100010"),
    BitPat("b??????? ????? ????? 010 ????? 00000 11") -> BitPat("b001001?00000000100010"),
    BitPat("b??????? ????? ????? 100 ????? 00000 11") -> BitPat("b001001?00000000100010"),
    BitPat("b??????? ????? ????? 101 ????? 00000 11") -> BitPat("b001001?00000000100010"),
    BitPat("b??????? ????? ????? 000 ????? 01000 11") -> BitPat("b000001?0000000??01001"),
    BitPat("b??????? ????? ????? 001 ????? 01000 11") -> BitPat("b000001?0000000??01001"),
    BitPat("b??????? ????? ????? 010 ????? 01000 11") -> BitPat("b000001?0000000??01001"),
    BitPat("b??????? ????? ????? 000 ????? 00100 11") -> BitPat("b001001?00000000000000"),
    BitPat("b??????? ????? ????? 010 ????? 00100 11") -> BitPat("b001001001110000000000"),
    BitPat("b??????? ????? ????? 011 ????? 00100 11") -> BitPat("b001001101110000000000"),
    BitPat("b??????? ????? ????? 100 ????? 00100 11") -> BitPat("b001001?10110000000000"),
    BitPat("b??????? ????? ????? 110 ????? 00100 11") -> BitPat("b001001?10100000000000"),
    BitPat("b??????? ????? ????? 111 ????? 00100 11") -> BitPat("b001001?10010000000000"),
    BitPat("b0000000 ????? ????? 001 ????? 00100 11") -> BitPat("b001001?00100000000000"),
    BitPat("b0000000 ????? ????? 101 ????? 00100 11") -> BitPat("b001001?00110000000000"),
    BitPat("b0100000 ????? ????? 101 ????? 00100 11") -> BitPat("b001001?01000000000000"),
    BitPat("b0000000 ????? ????? 000 ????? 01100 11") -> BitPat("b001000?000000000???00"),
    BitPat("b0100000 ????? ????? 000 ????? 01100 11") -> BitPat("b001000?000100000???00"),
    BitPat("b0000000 ????? ????? 001 ????? 01100 11") -> BitPat("b001000?001000000???00"),
    BitPat("b0000000 ????? ????? 010 ????? 01100 11") -> BitPat("b0010000011100000???00"),
    BitPat("b0000000 ????? ????? 011 ????? 01100 11") -> BitPat("b0010001011100000???00"),
    BitPat("b0000000 ????? ????? 100 ????? 01100 11") -> BitPat("b001000?101100000???00"),
    BitPat("b0000000 ????? ????? 101 ????? 01100 11") -> BitPat("b001000?001100000???00"),
    BitPat("b0100000 ????? ????? 101 ????? 01100 11") -> BitPat("b001000?010000000???00"),
    BitPat("b0000000 ????? ????? 110 ????? 01100 11") -> BitPat("b001000?101000000???00"),
    BitPat("b0000000 ????? ????? 111 ????? 01100 11") -> BitPat("b001000?100100000???00"),
    BitPat("b0000000 00000 00000 000 00000 11100 11") -> BitPat("b000????????000?????00"),
    BitPat("b0000000 00001 00000 000 00000 11100 11") -> BitPat("b010????????000?????00")
  )

  val table = TruthTable(lut, BitPat("b1????????????????????"))
  io.ctrl    := decoder(io.inst, table).asTypeOf(new Control)
  io.rd      := io.inst(11, 7)
  io.rs1     := io.inst(19, 15)
  io.rs2     := io.inst(24, 20)
  io.mem_len := io.inst(13, 12)
  io.load_U  := io.inst(14).asBool
}
