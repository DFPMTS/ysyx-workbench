import chisel3._
import chisel3.util._

class IFU extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new PC_Message))
    val out = Decoupled(new IFU_Message)
  })
  val inst_fetch   = Module(new InstFetch)
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

  inst_fetch.io.pc := data_buffer.pc
  io.out.bits.inst := inst_fetch.io.inst
  io.out.bits.pc   := data_buffer.pc
}
