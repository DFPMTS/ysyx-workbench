import chisel3._
import chisel3.util._
import scala.reflect.internal.Mode

class IDU extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new IFU_Message))
    val wb  = new WBSignal
    val out = Decoupled(new IDU_Out)
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

  io.out.bits.pc := data_buffer.pc

  val decode = Module(new Decode)
  decode.io.inst := data_buffer.inst
  val decodeSignals = decode.io.signals
  val ctrl          = Wire(new Control)

  val immgen = Module(new ImmGen)
  immgen.io.inst      := data_buffer.inst
  immgen.io.inst_type := decodeSignals.instType

  ctrl.invalid  := decodeSignals.invalid
  ctrl.regWe    := decodeSignals.regWe
  ctrl.aluFunc  := decodeSignals.aluFunc
  ctrl.fuType   := decodeSignals.fuType
  ctrl.fuOp     := decodeSignals.fuOp
  ctrl.src1Type := decodeSignals.src1Type
  ctrl.src2Type := decodeSignals.src2Type
  ctrl.rs1      := data_buffer.inst(19, 15)
  ctrl.rs2      := data_buffer.inst(24, 20)
  ctrl.rd       := data_buffer.inst(11, 7)

  io.out.bits.ctrl := ctrl
  io.out.bits.imm  := immgen.io.imm
  // io.out.bits.ctrl.          := immgen.io.imm.asUInt
  // io.out.bits.access_fault := data_buffer.access_fault
}
