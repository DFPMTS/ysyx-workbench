import chisel3._
import chisel3.util._

class RegFile extends Module {
  val io = IO(new Bundle {
    val rs1_sel = Input(UInt(5.W))
    val rs2_sel = Input(UInt(5.W))
    val wr_sel  = Input(UInt(5.W))
    val reg_we  = Input(UInt(1.W))
    val wb_data = Input(SInt(32.W))
    val rs1     = Output(SInt(32.W))
    val rs2     = Output(SInt(32.W))
  })

  val regs = RegInit(VecInit(Seq.fill(16)(0.S(32.W))))

  when(io.wr_sel =/= 0.U && io.reg_we.asBool) {
    regs(io.wr_sel) := io.wb_data
  }

  io.rs1 := regs(io.rs1_sel)
  io.rs2 := regs(io.rs2_sel)
}
