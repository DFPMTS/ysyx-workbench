module EBREAK (
    input ebreak
);

    import "DPI-C" function void nemu_break();
    always @(*) begin
        if (ebreak) nemu_break();
    end

endmodule
