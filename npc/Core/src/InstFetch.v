module InstFetch (
    input  [31:0] pc,
    output [31:0] inst
);
    import "DPI-C" function int inst_fetch(input int pc);

    assign inst = inst_fetch(pc);

endmodule
