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
  val cmp     = Module(new Comparator)
  val ebreak  = Module(new EBREAK)

  pc.io.dnpc := wb_pc.io.dnpc

  ifu.io.pc := pc.io.pc

  dec.io.inst := ifu.io.inst

  immgen.io.inst      := ifu.io.inst
  immgen.io.inst_type := dec.io.ctl.inst_type

  regfile.io.rs1_sel := dec.io.rs1
  regfile.io.rs2_sel := dec.io.rs2
  regfile.io.reg_we  := dec.io.ctl.reg_we
  regfile.io.wr_sel  := dec.io.rd
  regfile.io.wb_data := wb_reg.io.wb_data

  alu.io.rs1     := regfile.io.rs1
  alu.io.rs2     := regfile.io.rs2
  alu.io.imm     := immgen.io.imm
  alu.io.pc      := pc.io.pc
  alu.io.cmp_U   := dec.io.ctl.cmp_U
  alu.io.op1_sel := dec.io.ctl.op1_sel
  alu.io.op2_sel := dec.io.ctl.op2_sel
  alu.io.alu_sel := dec.io.ctl.alu_sel

  cmp.io.in1   := regfile.io.rs1
  cmp.io.in2   := regfile.io.rs2
  cmp.io.cmp_U := dec.io.ctl.cmp_U

  mem.io.addr   := alu.io.alu_out.asUInt
  mem.io.len    := dec.io.ctl.len
  mem.io.load_U := dec.io.ctl.load_U
  mem.io.mr     := dec.io.ctl.mr
  mem.io.mw     := dec.io.ctl.mw
  mem.io.data_w := regfile.io.rs2

  wb_pc.io.alu     := alu.io.alu_out.asUInt
  wb_pc.io.is_beq  := dec.io.ctl.is_beq
  wb_pc.io.is_bne  := dec.io.ctl.is_bne
  wb_pc.io.is_blt  := dec.io.ctl.is_blt
  wb_pc.io.is_bge  := dec.io.ctl.is_bge
  wb_pc.io.is_jump := dec.io.ctl.is_jump
  wb_pc.io.snpc    := pc.io.snpc
  wb_pc.io.lt      := cmp.io.lt
  wb_pc.io.ge      := cmp.io.ge
  wb_pc.io.eq      := cmp.io.eq

  wb_reg.io.alu    := alu.io.alu_out
  wb_reg.io.mem    := mem.io.data_r
  wb_reg.io.snpc   := pc.io.snpc.asSInt
  wb_reg.io.wb_sel := dec.io.ctl.wb_sel

  ebreak.io.ebreak := dec.io.ctl.break
}
