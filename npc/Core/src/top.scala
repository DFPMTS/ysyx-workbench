import chisel3._
import chisel3.util._

class top extends Module {
  val io      = IO(new Bundle {})
  val pc      = Module(new PC)
  val ifu     = Module(new InstFetch)
  val dec     = Module(new Decode)
  val immgen  = Module(new ImmGen)
  val regfile = Module(new RegFile)
  val alu     = Module(new ALU)
  val mem     = Module(new MEM)
  val wb_pc   = Module(new WB_PC)
  val wb_reg  = Module(new WB_REG)
  val ebreak  = Module(new EBREAK)

  val ctrl    = dec.io.ctrl
  val mem_len = dec.io.mem_len
  val load_U  = dec.io.load_U

  pc.io.dnpc := wb_pc.io.dnpc

  ifu.io.pc := pc.io.pc

  dec.io.inst := ifu.io.inst

  immgen.io.inst      := ifu.io.inst
  immgen.io.inst_type := ctrl.inst_type

  regfile.io.rs1_sel := dec.io.rs1
  regfile.io.rs2_sel := dec.io.rs2
  regfile.io.reg_we  := ctrl.reg_we
  regfile.io.wr_sel  := dec.io.rd
  regfile.io.wb_data := wb_reg.io.wb_data

  val rs1     = regfile.io.rs1
  val rs2     = regfile.io.rs2
  val imm     = immgen.io.imm.asUInt
  val alu_op1 = MuxLookup(ctrl.alu_sel1, 0.U)(Seq("b00".U -> rs1, "b01".U -> pc.io.pc, "b10".U -> 0.U))
  val alu_op2 = MuxLookup(ctrl.alu_sel2, 0.U)(Seq("b0".U -> rs2, "b1".U -> imm))

  alu.io.op1      := alu_op1
  alu.io.op2      := alu_op2
  alu.io.cmp_U    := ctrl.cmp_U
  alu.io.alu_func := ctrl.alu_func

  mem.io.addr   := alu.io.out
  mem.io.len    := mem_len
  mem.io.load_U := load_U
  mem.io.mr     := ctrl.mr
  mem.io.mw     := ctrl.mw
  mem.io.data_w := rs2

  val target = Mux(ctrl.jalr.asBool, rs1 + imm, pc.io.pc + imm)
  wb_pc.io.target      := target
  wb_pc.io.snpc        := pc.io.snpc
  wb_pc.io.jal         := ctrl.jal
  wb_pc.io.jalr        := ctrl.jalr
  wb_pc.io.take_branch := ctrl.branch & alu.io.cmp_out

  wb_reg.io.alu    := alu.io.out
  wb_reg.io.mem    := mem.io.data_r
  wb_reg.io.snpc   := pc.io.snpc
  wb_reg.io.wb_sel := ctrl.wb_sel

  ebreak.io.ebreak := ctrl.ebreak
}
