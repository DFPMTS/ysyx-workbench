import chisel3._
import chisel3.util._

class MemRead extends HasBlackBoxInline {
  val io = IO(new Bundle {
    val clk    = Input(Clock())
    val addr   = Input(UInt(32.W))
    val en     = Input(UInt(1.W))
    val data_r = Output(UInt(32.W))
  })
  // addPath("Core/src/MemRead.v")
  setInline(
    "MemRead.v",
    """module MemRead (
      |    input clk,
      |    input  [31:0] addr,
      |    input         en,
      |    output reg [31:0] data_r
      |);
      |    // synopsys translate_off
      |    import "DPI-C" function int mem_read(input int addr);
      |    
      |    always @(posedge clk) begin
      |        if (en) data_r = mem_read(addr);
      |        else data_r =  32'b0;
      |    end
      |    // synopsys translate_on
      |
      |endmodule
  """.stripMargin
  )
}

class MemWrite extends HasBlackBoxInline {
  val io = IO(new Bundle {
    val clk   = Input(Clock())
    val addr  = Input(UInt(32.W))
    val wdata = Input(UInt(32.W))
    val en    = Input(UInt(1.W))
    val wmask = Input(UInt(4.W))
  })
  setInline(
    "MemWrite.v",
    """module MemWrite (
      |    input clk,
      |    input [31:0] addr,
      |    input [31:0] wdata,
      |    input        en,
      |    input [ 3:0] wmask
      |);
      |    // synopsys translate_off
      |    import "DPI-C" function void mem_write(
      |        input int  addr,
      |        input int  wdata,
      |        input byte wmask
      |    );
      |    always @(posedge clk) begin
      |        if (en) mem_write(addr, wdata, {4'b0,wmask});
      |    end
      |    // synopsys translate_on
      |
      |endmodule
      |
  """.stripMargin
  )
}
