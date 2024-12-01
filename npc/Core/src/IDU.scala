import chisel3._
import chisel3.util._
import scala.reflect.internal.Mode

class IDU extends Module with HasDecodeConstants with HasPerfCounters {
  val io = IO(new Bundle {
    val in       = Flipped(Decoupled(new InstSignal))
    val EXBypass = Input(new WBSignal)
    val wb       = Input(new WBSignal)
    val out      = Decoupled(new IDU_Message)
    val flush    = Input(Bool())
  })
  val counter = RegInit(0.U(3.W))
  val insert  = Wire(Bool())
  // val inBuffer = RegEnable(io.in.bits, insert)
  val inBuffer = io.in.bits
  // val validBuffer = RegEnable(io.in.valid, insert)
  val validBuffer = io.in.valid
  counter := Mux(
    io.in.fire,
    0.U,
    Mux(counter === 0.U, 0.U, counter - 1.U)
  )
  insert := ~validBuffer || (counter === 0.U && io.out.ready)

  // io.in.ready  := insert
  io.in.ready  := io.out.ready
  io.out.valid := validBuffer && counter === 0.U && !io.flush

  val ctrl = Wire(new ControlSignal)
  val data = Wire(new DataSignal)
  data.out := DontCare
  val decodeSignal = Wire(new DecodeSignal)
  data.pc := inBuffer.pc

  val regfile = Module(new RegFile)
  regfile.io.wb     := io.wb
  regfile.io.rs1Sel := ctrl.rs1
  regfile.io.rs2Sel := ctrl.rs2
  val rs1Val = io.EXBypass.tryBypass(ctrl.rs1, regfile.io.rs1)
  val rs2Val = io.EXBypass.tryBypass(ctrl.rs2, regfile.io.rs2)
  data.src1   := MuxLookup(ctrl.src1Type, 0.U)(Seq(REG -> rs1Val, PC -> inBuffer.pc, ZERO -> 0.U))
  data.src2   := MuxLookup(ctrl.src2Type, 0.U)(Seq(REG -> rs2Val, IMM -> data.imm, ZERO -> 0.U))
  data.rs2Val := rs2Val

  val immgen = Module(new ImmGen)
  immgen.io.inst      := inBuffer.inst
  immgen.io.inst_type := decodeSignal.instType
  data.imm            := immgen.io.imm

  val decode = Module(new Decode)
  decodeSignal   := decode.io.signals
  decode.io.inst := inBuffer.inst
  ctrl.inst      := inBuffer.inst
  ctrl.invalid   := decodeSignal.invalid
  ctrl.regWe     := decodeSignal.regWe
  ctrl.aluFunc   := decodeSignal.aluFunc
  ctrl.fuType    := decodeSignal.fuType
  ctrl.fuOp      := decodeSignal.fuOp
  ctrl.src1Type  := decodeSignal.src1Type
  ctrl.src2Type  := decodeSignal.src2Type
  ctrl.rs1       := inBuffer.inst(19, 15)
  ctrl.rs2       := inBuffer.inst(24, 20)
  ctrl.rd        := inBuffer.inst(11, 7)

  io.out.bits.ctrl := ctrl
  io.out.bits.data := data

  monitorEvent(iduAluInst, io.out.fire && ctrl.fuType === ALU)
  monitorEvent(iduMemInst, io.out.fire && ctrl.fuType === MEM)
  monitorEvent(iduBruInst, io.out.fire && ctrl.fuType === BRU)
  monitorEvent(iduCsrInst, io.out.fire && ctrl.fuType === CSR)
}

class testIDU extends Module with HasDecodeConstants {
  val io = IO(new Bundle {
    val out = UInt(32.W)
  })

  val r   = Reg(UInt(32.W))
  val idu = Module(new IDU)
  idu.io.in.bits.inst         := r
  idu.io.in.bits.pc           := r
  idu.io.in.bits.access_fault := true.B
  idu.io.in.valid             := true.B

  idu.io.wb.data := 0.U
  idu.io.wb.rd   := 0.U
  idu.io.wb.wen  := 0.U

  r := idu.io.out.bits.ctrl.aluFunc & idu.io.out.bits.data.imm & idu.io.out.bits.data.src1 & idu.io.out.bits.data.pc

  idu.io.out.ready := true.B

  io.out := r
}
