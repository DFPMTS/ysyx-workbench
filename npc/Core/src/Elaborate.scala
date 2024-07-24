import circt.stage._
import chisel3._

object parseArgs {
  def apply(args: Array[String]) = {
    if (args.contains("Debug")) {
      Config.debug = true
    } else {
      Config.debug = false
    }
    if (args.contains("npc")) {
      Config.resetPC = "h80000000".U
    } else {
      Config.resetPC = "h30000000".U
    }
  }
}

object Elaborate_npc extends App {
  parseArgs(args)
  def top       = new npc_top
  val generator = Seq(chisel3.stage.ChiselGeneratorAnnotation(() => top))
  (new ChiselStage)
    .execute(Array("-td", "./vsrc", "--split-verilog"), generator :+ CIRCTTargetAnnotation(CIRCTTarget.Verilog))
}

object Elaborate_soc extends App {
  parseArgs(args)
  def top       = new multi
  val generator = Seq(chisel3.stage.ChiselGeneratorAnnotation(() => top))
  (new ChiselStage)
    .execute(Array("-td", "./vsrc", "--split-verilog"), generator :+ CIRCTTargetAnnotation(CIRCTTarget.Verilog))
}

object Elaborate_nvboard extends App {
  parseArgs(args)
  def top       = new multi
  val generator = Seq(chisel3.stage.ChiselGeneratorAnnotation(() => top))
  (new ChiselStage)
    .execute(Array("-td", "./vsrc", "--split-verilog"), generator :+ CIRCTTargetAnnotation(CIRCTTarget.Verilog))
}
