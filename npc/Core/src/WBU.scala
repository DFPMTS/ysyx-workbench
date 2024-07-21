import chisel3._
import chisel3.util._
import dataclass.data

trait HasCSROps {
  def CSRW   = 0.U(4.W)
  def CSRS   = 1.U(4.W)
  def ECALL  = 2.U(4.W)
  def MRET   = 3.U(4.W)
  def EBREAK = 4.U(4.W)
}

class WBU extends Module with HasDecodeConstants {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new MEM_Message))
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

  val isCSR    = ctrl.fuType === CSR
  val isCSRW   = isCSR && ctrl.fuOp === CSRW
  val isCSRS   = isCSR && ctrl.fuOp === CSRS
  val isECALL  = isCSR && ctrl.fuOp === ECALL
  val isMRET   = isCSR && ctrl.fuOp === MRET
  val isEBREAK = isCSR && ctrl.fuOp === EBREAK

  csr.io.ren   := isCSRS && rd =/= 0.U && valid_buffer
  csr.io.addr  := data_buffer.imm(11, 0)
  csr.io.wen   := isCSRW && rs1 =/= 0.U && valid_buffer
  csr.io.wtype := wtype
  csr.io.wdata := data_buffer.rs1
  csr.io.ecall := isECALL && valid_buffer
  csr.io.pc    := data_buffer.pc

  val wb_pc    = Module(new WB_PC)
  val snpc     = data_buffer.pc + 4.U
  val target   = data_buffer.alu_out
  val isJUMP   = ctrl.fuType === BRU && ctrl.fuOp === JUMP
  val isBRANCH = ctrl.fuType === BRU && ctrl.fuOp === BRANCH
  wb_pc.io.mtvec       := csr.io.mtvec
  wb_pc.io.ecall       := isECALL.asBool
  wb_pc.io.mepc        := csr.io.mepc
  wb_pc.io.mret        := isMRET.asBool
  wb_pc.io.target      := target
  wb_pc.io.snpc        := snpc
  wb_pc.io.jal         := isJUMP
  wb_pc.io.jalr        := isJUMP
  wb_pc.io.take_branch := isBRANCH && data_buffer.alu_cmp_out
  io.out.bits.dnpc     := wb_pc.io.dnpc

  val wb_reg = Module(new WB_REG)
  wb_reg.io.alu       := data_buffer.alu_out
  wb_reg.io.csr       := csr.io.rdata
  wb_reg.io.mem       := data_buffer.mem_out
  wb_reg.io.snpc      := snpc
  wb_reg.io.wb_sel    := ctrl.fuType
  io.out.bits.wb_data := wb_reg.io.wb_data

  io.out.bits.reg_we       := data_buffer.ctrl.regWe
  io.out.bits.pc           := data_buffer.pc
  io.out.bits.inst         := data_buffer.inst
  io.out.bits.ebreak       := isEBREAK
  io.out.bits.access_fault := data_buffer.access_fault
  io.out.bits.invalid_inst := ctrl.invalid.asBool
}
