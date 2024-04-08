module MemRead (
    input  [31:0] addr,
    input         en,
    output [31:0] data_r
);

    import "DPI-C" function int mem_read(input int addr);

    assign data_r = en ? mem_read(addr) : 32'b0;

endmodule
