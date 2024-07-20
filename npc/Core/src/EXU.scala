import chisel3._
import chisel3.util._
import dataclass.data

class EXU extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new IDU_Message))
    val out = Decoupled(new EXU_Message)
  })
  val insert       = Wire(Bool())
  val data_buffer  = RegEnable(io.in.bits, insert)
  val valid_buffer = RegEnable(io.in.valid, insert)

  insert := ~valid_buffer || io.out.fire

  io.in.ready := insert

  val ctrl = data_buffer.ctrl

  // -------------------------- ALU --------------------------
  val alu = Module(new ALU)
  val alu_op1 =
    MuxLookup(ctrl.alu_sel1, 0.U)(Seq("b00".U -> data_buffer.rs1, "b01".U -> data_buffer.pc, "b10".U -> 0.U))
  val alu_op2 = MuxLookup(ctrl.alu_sel2, 0.U)(Seq("b0".U -> data_buffer.rs2, "b1".U -> data_buffer.imm))
  alu.io.alu_func := ctrl.alu_func
  alu.io.cmp_U    := ctrl.cmp_U
  alu.io.op1      := alu_op1
  alu.io.op2      := alu_op2

  io.out.bits.alu_out     := alu.io.out
  io.out.bits.alu_cmp_out := alu.io.cmp_out

  io.out.bits.imm          := data_buffer.imm
  io.out.bits.rs1          := data_buffer.rs1
  io.out.bits.rs2          := data_buffer.rs2
  io.out.bits.ctrl         := data_buffer.ctrl
  io.out.bits.pc           := data_buffer.pc
  io.out.bits.inst         := data_buffer.inst
  io.out.bits.access_fault := data_buffer.access_fault
  io.out.valid             := valid_buffer
}
