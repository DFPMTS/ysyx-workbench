import chisel3._
import chisel3.util._

class MemRead extends HasBlackBoxInline {
  val io = IO(new Bundle {
    val clk    = Input(Clock())
    val addr   = Input(UInt(32.W))
    val en     = Input(UInt(1.W))
    val data_r = Output(UInt(64.W))
  })
  // addPath("Core/src/MemRead.v")
  setInline(
    "MemRead.v",
    """module MemRead (
      |    input clk,
      |    input  [31:0] addr,
      |    input         en,
      |    output reg [63:0] data_r
      |);
      |
      |    import "DPI-C" function longint mem_read(input int addr);
      |    
      |    always @(posedge clk) begin
      |        if (en) data_r = mem_read(addr);
      |        else data_r =  64'b0;
      |    end
      |
      |endmodule
  """.stripMargin
  )
}

class MemWrite extends HasBlackBoxInline {
  val io = IO(new Bundle {
    val clk   = Input(Clock())
    val addr  = Input(UInt(32.W))
    val wdata = Input(UInt(64.W))
    val en    = Input(UInt(1.W))
    val wmask = Input(UInt(8.W))
  })
  setInline(
    "MemWrite.v",
    """module MemWrite (
      |    input clk,
      |    input [31:0] addr,
      |    input [63:0] wdata,
      |    input        en,
      |    input [ 7:0] wmask
      |);
      |
      |    import "DPI-C" function void mem_write(
      |        input int  addr,
      |        input longint  wdata,
      |        input byte wmask
      |    );
      |    always @(posedge clk) begin
      |        if (en) mem_write(addr, wdata, wmask);
      |    end
      |
      |endmodule
      |
  """.stripMargin
  )
}
