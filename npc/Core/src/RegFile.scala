import chisel3._
import chisel3.util._

class RegFile extends Module {
  val io = IO(new Bundle {
    val rs1Sel = Input(UInt(4.W))
    val rs2Sel = Input(UInt(4.W))
    val wb     = Input(new WBSignal)
    val rs1    = Output(UInt(32.W))
    val rs2    = Output(UInt(32.W))
  })

  val regs = RegInit({
    val regs = Wire(Vec(16, UInt(32.W)))
    regs    := DontCare
    regs(0) := 0.U
    regs
  })

  when(io.wb.rd =/= 0.U && io.wb.wen) {
    regs(io.wb.rd) := io.wb.data
  }

  io.rs1 := io.wb.tryBypass(io.rs1Sel, regs(io.rs1Sel))
  io.rs2 := io.wb.tryBypass(io.rs2Sel, regs(io.rs2Sel))
}
