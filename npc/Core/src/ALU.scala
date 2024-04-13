import chisel3._
import chisel3.util._

class ALU extends Module {
  val io = IO(new Bundle {
    val op1      = Input(UInt(32.W))
    val op2      = Input(UInt(32.W))
    val alu_func = Input(UInt(4.W))
    val cmp_U    = Input(UInt(1.W))
    val out      = Output(UInt(32.W))
    val cmp_out  = Output(Bool())
  })
  val add :: sub :: left :: right :: arith :: eq :: ne :: lt :: ge :: and :: or :: xor :: Nil =
    Enum(12)

  // [adder] add / sub
  val is_sub    = ~(io.alu_func === add) // for cmp
  val op2_adder = Mux(is_sub, ~io.op2, io.op2)
  val adder_res = io.op1 + op2_adder + is_sub

  // [shift] left / right / arith
  val shamt = io.op2(4, 0)
  // [logic] and / or / xor
  val xor_res = io.op1 ^ io.op2
  // [cmp] eq / ne / lt / ge
  val eq_res = xor_res === 0.U
  val ne_res = ~eq_res
  /*                 lt
     op1_msb | op2_msb |   U   |   S
        0         0     sub_msb sub_msb
        1         1     sub_msb sub_msb
        0         1        1       0
        1         0        0       1
   */
  val op1_msb = io.op1(31);
  val op2_msb = io.op2(31);
  val sub_msb = adder_res(31);
  val lt_res  = Mux(op1_msb === op2_msb, sub_msb, Mux(io.cmp_U.asBool, op2_msb, op1_msb))
  val ge_res  = ~lt_res

  io.out := adder_res
  switch(io.alu_func) {
    is(left) {
      io.out := (io.op1 << shamt)
    }
    is(right) {
      io.out := (io.op1 >> shamt)
    }
    is(arith) {
      io.out := (io.op1.asSInt >> shamt).asUInt
    }
    is(eq) {
      io.out := eq_res
    }
    is(ne) {
      io.out := ne_res
    }
    is(lt) {
      io.out := lt_res
    }
    is(ge) {
      io.out := ge_res
    }
    is(and) {
      io.out := io.op1 & io.op2
    }
    is(or) {
      io.out := io.op1 | io.op2
    }
    is(xor) {
      io.out := xor_res
    }
  }
  io.cmp_out := io.out(0)
}
