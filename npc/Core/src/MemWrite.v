module MemWrite (
    input [31:0] addr,
    input [31:0] wdata,
    input        en,
    input [ 3:0] wmask
);

    import "DPI-C" function void mem_write(
        input int  addr,
        input int  wdata,
        input byte wmask
    );
    always @(*) begin
        if (en) mem_write(addr, wdata, {4'b0, wmask});
    end

endmodule
