import chisel3._
import chisel3.util._
import dataclass.data

class EXU extends Module {
  val io = IO(new Bundle {
    val in     = Flipped(Decoupled(new IDU_Message))
    val out    = Decoupled(new EXU_Message)
    val master = new AXI4(64, 32)
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

  // -------------------------- MEM --------------------------
  val invalid        = io.in.bits.access_fault || io.in.bits.ctrl.invalid.asBool
  val invalid_buffer = RegEnable(invalid, insert)
  val mem_len        = data_buffer.inst(13, 12)
  val load_U         = data_buffer.inst(14).asBool
  val is_mem         = ctrl.mr | ctrl.mw

  // ar_valid/aw_valid/w_valid 当一个valid请求进入时置为true,在相应通道握手后为false
  val ar_valid = RegInit(false.B)
  ar_valid := Mux(
    insert,
    io.in.valid && io.in.bits.ctrl.mr.asBool && !invalid,
    Mux(io.master.ar.fire, false.B, ar_valid)
  )
  val addr        = alu.io.out
  val addr_offset = addr(2, 0);
  io.master.ar.valid      := ar_valid
  io.master.ar.bits.addr  := addr
  io.master.ar.bits.id    := 0.U
  io.master.ar.bits.len   := 0.U
  io.master.ar.bits.size  := mem_len
  io.master.ar.bits.burst := "b01".U
  io.master.r.ready       := Mux(valid_buffer && ctrl.mr.asBool, io.out.ready, false.B)

  val aw_valid = RegInit(false.B)
  aw_valid := Mux(
    insert,
    io.in.valid && io.in.bits.ctrl.mw.asBool && !invalid,
    Mux(io.master.aw.fire, false.B, aw_valid)
  )
  io.master.aw.valid      := aw_valid
  io.master.aw.bits.addr  := addr
  io.master.aw.bits.id    := 0.U
  io.master.aw.bits.len   := 0.U
  io.master.aw.bits.size  := mem_len
  io.master.aw.bits.burst := "b01".U

  val w_valid = RegInit(false.B)
  w_valid := Mux(
    insert,
    io.in.valid && io.in.bits.ctrl.mw.asBool && !invalid,
    Mux(io.master.w.fire, false.B, w_valid)
  )
  io.master.w.valid     := ctrl.mw & w_valid
  io.master.w.bits.data := data_buffer.rs2 << (addr_offset << 3.U)
  io.master.w.bits.strb := MuxLookup(mem_len, 0.U(4.W))(
    Seq(
      0.U(2.W) -> "b0001".U,
      1.U(2.W) -> "b0011".U,
      2.U(2.W) -> "b1111".U
    )
  ) << addr_offset
  io.master.w.bits.last := true.B
  io.master.b.ready     := Mux(valid_buffer && ctrl.mw.asBool, io.out.ready, false.B)

  val raw_data      = io.master.r.bits.data >> (addr_offset << 3.U)
  val sign_ext_data = WireDefault(raw_data)
  when(mem_len === "b00".U) {
    sign_ext_data := Cat(Fill(24, ~load_U & raw_data(7)), raw_data(7, 0))
  }.elsewhen(mem_len === "b01".U) {
    sign_ext_data := Cat(Fill(16, ~load_U & raw_data(15)), raw_data(15, 0))
  }

  io.out.bits.mem_out := sign_ext_data
  io.out.valid := Mux(
    is_mem.asBool && !invalid_buffer,
    Mux(ctrl.mr.asBool, io.master.r.valid, io.master.b.valid),
    valid_buffer
  )

  // ---------------------------------------------------------

  io.out.bits.imm  := data_buffer.imm
  io.out.bits.rs1  := data_buffer.rs1
  io.out.bits.rs2  := data_buffer.rs2
  io.out.bits.ctrl := data_buffer.ctrl
  io.out.bits.pc   := data_buffer.pc
  io.out.bits.inst := data_buffer.inst
  io.out.bits.access_fault := data_buffer.access_fault || Mux(
    !invalid_buffer && ctrl.mr.asBool,
    io.master.r.bits.resp =/= 0.U,
    io.master.b.bits.resp =/= 0.U
  )
}
