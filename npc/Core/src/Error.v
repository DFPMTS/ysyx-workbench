module Error (
    input ebreak,
    input access_fault,
    input invalid_inst
);

    import "DPI-C" function void raise_ebreak();
    import "DPI-C" function void raise_access_fault();
    import "DPI-C" function void raise_invalid_inst();
    always @(*) begin
        if (ebreak) raise_ebreak();
        if (access_fault) raise_access_fault();
        if (invalid_inst) raise_invalid_inst();

    end

endmodule
