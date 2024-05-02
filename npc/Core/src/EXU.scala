import chisel3._
import chisel3.util._
import dataclass.data

class EXU extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new IDU_Message))
    val out = Decoupled(new EXU_Message)
  })
  val counter      = RegInit(0.U(3.W))
  val insert       = Wire(Bool())
  val data_buffer  = RegEnable(io.in.bits, insert)
  val valid_buffer = RegEnable(io.in.valid, insert)
  counter := Mux(
    io.in.fire,
    0.U,
    Mux(counter === 0.U, 0.U, counter - 1.U)
  )
  insert := ~valid_buffer || (counter === 0.U && io.out.ready)

  io.in.ready  := insert
  io.out.valid := valid_buffer && counter === 0.U

  val ctrl = data_buffer.ctrl
  val alu  = Module(new ALU)
  val alu_op1 =
    MuxLookup(ctrl.alu_sel1, 0.U)(Seq("b00".U -> data_buffer.rs1, "b01".U -> data_buffer.pc, "b10".U -> 0.U))
  val alu_op2 = MuxLookup(ctrl.alu_sel2, 0.U)(Seq("b0".U -> data_buffer.rs2, "b1".U -> data_buffer.imm))
  alu.io.alu_func := ctrl.alu_func
  alu.io.cmp_U    := ctrl.cmp_U
  alu.io.op1      := alu_op1
  alu.io.op2      := alu_op2

  io.out.bits.alu_out     := alu.io.out
  io.out.bits.alu_cmp_out := alu.io.cmp_out

  val mem     = Module(new MEM)
  val mem_len = data_buffer.inst(13, 12)
  val load_U  = data_buffer.inst(14).asBool
  mem.io.addr   := alu.io.out
  mem.io.len    := mem_len
  mem.io.load_U := load_U
  mem.io.mr     := ctrl.mr & valid_buffer
  mem.io.mw     := ctrl.mw & valid_buffer
  mem.io.data_w := data_buffer.rs2

  io.out.bits.mem_out := mem.io.data_r

  io.out.bits.imm  := data_buffer.imm
  io.out.bits.rs1  := data_buffer.rs1
  io.out.bits.rs2  := data_buffer.rs2
  io.out.bits.ctrl := data_buffer.ctrl
  io.out.bits.pc   := data_buffer.pc
  io.out.bits.inst := data_buffer.inst
}
