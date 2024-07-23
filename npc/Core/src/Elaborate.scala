import circt.stage._

object Elaborate_npc extends App {
  if (args.contains("Debug")) {
    Config.debug = true
  } else {
    Config.debug = false
  }
  def top       = new npc_top
  val generator = Seq(chisel3.stage.ChiselGeneratorAnnotation(() => top))
  (new ChiselStage)
    .execute(Array("-td", "./vsrc", "--split-verilog"), generator :+ CIRCTTargetAnnotation(CIRCTTarget.Verilog))
}

object Elaborate_soc extends App {
  def top       = new multi
  val generator = Seq(chisel3.stage.ChiselGeneratorAnnotation(() => top))
  (new ChiselStage)
    .execute(Array("-td", "./vsrc", "--split-verilog"), generator :+ CIRCTTargetAnnotation(CIRCTTarget.Verilog))
}

object Elaborate_nvboard extends App {
  def top       = new multi
  val generator = Seq(chisel3.stage.ChiselGeneratorAnnotation(() => top))
  (new ChiselStage)
    .execute(Array("-td", "./vsrc", "--split-verilog"), generator :+ CIRCTTargetAnnotation(CIRCTTarget.Verilog))
}
