import chisel3._
import chisel3.util._

class ALU extends Module {
  val io = IO(new Bundle {
    val pc      = Input(UInt(32.W))
    val rs1     = Input(SInt(32.W))
    val rs2     = Input(SInt(32.W))
    val op1_sel = Input(UInt(2.W))
    val op2_sel = Input(UInt(1.W))
    val alu_sel = Input(UInt(4.W))
    val imm     = Input(SInt(32.W))
    val cmp_U   = Input(UInt(1.W))
    val alu_out = Output(SInt(32.W))
  })
  val add :: sub :: left :: right :: arith :: lt :: and :: or :: xor :: mul :: mulh :: div :: rem :: Nil = Enum(13)

  val op1 = MuxLookup(io.op1_sel, 0.S)(Seq("b00".U -> io.rs1, "b01".U -> io.pc.asSInt, "b10".U -> 0.S))
  val op2 = MuxLookup(io.op2_sel, 0.S)(Seq("b0".U -> io.rs2, "b1".U -> io.imm))

  io.alu_out := 0.S(32.W)
  switch(io.alu_sel) {
    is(add) {
      io.alu_out := op1 + op2
    }
    is(sub) {
      io.alu_out := op1 - op2
    }
    is(left) {
      io.alu_out := (op1.asUInt << op2(4, 0)).asSInt
    }
    is(right) {
      io.alu_out := (op1.asUInt >> op2(4, 0)).asSInt
    }
    is(arith) {
      io.alu_out := op1 >> op2(4, 0)
    }
    is(lt) {
      io.alu_out := Mux(io.cmp_U.asBool, (op1.asUInt < op2.asUInt).zext, (op1 < op2).zext)
    }
    is(and) {
      io.alu_out := op1 & op2
    }
    is(or) {
      io.alu_out := op1 | op2
    }
    is(xor) {
      io.alu_out := op1 ^ op2
    }
    is(mul) {
      io.alu_out := op1 * op2
    }
    is(mulh) {
      io.alu_out := (op1 * op2)(63, 32).asSInt
    }
    is(div) {
      io.alu_out := op1 / op2
    }
    is(rem) {
      io.alu_out := op1 % op2
    }
  }
}
