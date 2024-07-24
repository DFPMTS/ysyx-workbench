import chisel3._
import chisel3.util._

class EventMonitor extends Module {
  val io = IO(new Bundle {
    val eventId = Input(UInt(Config.eventIdWidth))
    val enable  = Input(Bool())
  })
  val monitorEventHelper = Module(new EventMonitorHelper)
  monitorEventHelper.io.clk     := clock.asBool
  monitorEventHelper.io.eventId := io.eventId
  monitorEventHelper.io.enable  := io.enable
}

class EventMonitorHelper extends HasBlackBoxInline {
  val io = IO(new Bundle {
    val clk     = Input(Bool())
    val eventId = Input(UInt(Config.eventIdWidth))
    val enable  = Input(Bool())
  })
  setInline(
    "EventMonitorHelper.v",
    """module EventMonitorHelper (
      |    input clk,
      |    input [5:0] eventId,
      |    input enable
      |);
      |    // synopsys translate_off
      |    import "DPI-C" function void monitorEvent(
      |        input int eventId,
      |        input int enable
      |    );
      |    always @(posedge clk) begin
      |        monitorEvent({26'b0,eventId}, {31'b0,enable});
      |    end
      |    // synopsys translate_on
      |
      |endmodule
  """.stripMargin
  )
}
