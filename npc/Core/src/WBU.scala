import chisel3._
import chisel3.util._
import dataclass.data

class WBU extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new EXU_Message))
    val out = Decoupled(new WBU_Message)
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

  val ctrl  = data_buffer.ctrl
  val csr   = Module(new CSR)
  val rd    = data_buffer.inst(11, 7)
  val rs1   = data_buffer.inst(19, 15)
  val wtype = data_buffer.inst(13, 12)
  csr.io.ren   := ctrl.csr.asBool && rd =/= 0.U && valid_buffer
  csr.io.addr  := data_buffer.imm(11, 0)
  csr.io.wen   := ctrl.csr.asBool && rs1 =/= 0.U && valid_buffer
  csr.io.wtype := wtype
  csr.io.wdata := data_buffer.rs1
  csr.io.ecall := ctrl.ecall.asBool
  csr.io.pc    := data_buffer.pc

  val wb_pc  = Module(new WB_PC)
  val snpc   = data_buffer.pc + 4.U
  val target = Mux(ctrl.jalr.asBool, data_buffer.rs1, data_buffer.pc) + data_buffer.imm
  wb_pc.io.mtvec       := csr.io.mtvec
  wb_pc.io.ecall       := ctrl.ecall.asBool
  wb_pc.io.mepc        := csr.io.mepc
  wb_pc.io.mret        := ctrl.mret.asBool
  wb_pc.io.target      := target
  wb_pc.io.snpc        := snpc
  wb_pc.io.jal         := ctrl.jal
  wb_pc.io.jalr        := ctrl.jalr
  wb_pc.io.take_branch := ctrl.branch.asBool && data_buffer.alu_cmp_out
  io.out.bits.dnpc     := wb_pc.io.dnpc

  val wb_reg = Module(new WB_REG)
  wb_reg.io.alu       := data_buffer.alu_out
  wb_reg.io.csr       := csr.io.rdata
  wb_reg.io.mem       := data_buffer.mem_out
  wb_reg.io.snpc      := snpc
  wb_reg.io.wb_sel    := ctrl.wb_sel
  io.out.bits.wb_data := wb_reg.io.wb_data

  io.out.bits.reg_we := data_buffer.ctrl.reg_we
  io.out.bits.pc     := data_buffer.pc
  io.out.bits.inst   := data_buffer.inst
  io.out.bits.ebreak := ctrl.ebreak
}
