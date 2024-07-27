import chisel3._
import chisel3.util._

class RegFile extends Module {
  val io = IO(new Bundle {
    val rs1_sel = Input(UInt(5.W))
    val rs2_sel = Input(UInt(5.W))
    val wr_sel  = Input(UInt(5.W))
    val reg_we  = Input(UInt(1.W))
    val wb_data = Input(UInt(32.W))
    val rs1     = Output(UInt(32.W))
    val rs2     = Output(UInt(32.W))
  })

  val regs = Reg(Vec(16, UInt(32.W)))

  when(io.wr_sel =/= 0.U && io.reg_we.asBool) {
    regs(io.wr_sel(3, 0)) := io.wb_data
  }

  io.rs1 := regs(io.rs1_sel(3, 0))
  io.rs2 := regs(io.rs2_sel(3, 0))
}
