import chisel3._
import chisel3.util._
import scala.reflect.internal.Mode

class IDUIO extends Bundle {
  val IN_inst       = Flipped(Decoupled(new InstSignal))
  val OUT_decodeUop = Decoupled(new DecodeUop)
  val IN_flush    = Input(Bool())
}

class IDU extends Module with HasDecodeConstants with HasPerfCounters {
  val io = IO(new IDUIO)

  // * Submodules
  val immgenModule = Module(new ImmGen)
  val decodeModule = Module(new Decode)

  // * Main Signals
  val uopValid = RegInit(false.B)
  val uopReg = Reg(new DecodeUop)
  val uopNext = Wire(new DecodeUop)

  // * Dataflow
  // ** Input
  val inst = io.IN_inst.bits.inst
  val pc = io.IN_inst.bits.pc
  val rd = inst(11, 7)
  val rs1 = inst(19, 15)
  val rs2 = inst(24, 20)
  
  // *** Immediate Generation  
  immgenModule.io.inst      := io.IN_inst.bits.inst
  immgenModule.io.inst_type := decodeSignal.instType
  val imm = immgenModule.io.imm

  // *** Control Signals Generation  
  decodeModule.io.inst := io.IN_inst.bits.inst
  val decodeSignal     = decodeModule.io.signals

  // *** Filling uopNext
  uopNext.rd        := Mux(decodeSignal.regWe, rd, ZERO)
  uopNext.rs1       := rs1
  uopNext.rs2       := rs2

  uopNext.src1Type  := decodeSignal.src1Type
  uopNext.src2Type  := decodeSignal.src2Type  

  uopNext.fuType    := decodeSignal.fuType
  uopNext.opcode    := decodeSignal.fuOp
  
  uopNext.predTarget := pc + 4.U
  uopNext.pc        := pc
  uopNext.imm       := imm
  // * Control
  // ** Input
  val inValid = io.IN_inst.valid  
  val inFire = io.IN_inst.fire
  val outReady = io.OUT_decodeUop.ready  

  // ** IN ready generation
  val inReady = !uopValid || outReady

  // ** Update Logic
  when (io.IN_flush) {
    uopValid := false.B
  }.otherwise{
    uopValid := Mux(inReady, inValid, uopValid)  
  }  
  uopReg := Mux(inFire, uopNext, uopReg)

  // * Output Logic
  io.IN_inst.ready := inReady
  io.OUT_decodeUop.valid := uopValid
  io.OUT_decodeUop.bits := uopReg

  // monitorEvent(iduAluInst, io.out.fire && ctrl.fuType === ALU)
  // monitorEvent(iduMemInst, io.out.fire && ctrl.fuType === MEM)
  // monitorEvent(iduBruInst, io.out.fire && ctrl.fuType === BRU)
  // monitorEvent(iduCsrInst, io.out.fire && ctrl.fuType === CSR)
}