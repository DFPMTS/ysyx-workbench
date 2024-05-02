// import chisel3._
// import chisel3.util._

// class top extends Module {
//   val io = IO(new Bundle {
//     val rs1  = Output(UInt(5.W))
//     val rs2  = Output(UInt(5.W))
//     val rd   = Output(UInt(5.W))
//     val jalr = Output(UInt(1.W))
//     val jal  = Output(UInt(1.W))
//     val imm  = Output(UInt(32.W))
//   })
//   val ifu     = Module(new InstFetch)
//   val dec     = Module(new Decode)
//   val immgen  = Module(new ImmGen)
//   val regfile = Module(new RegFile)
//   val alu     = Module(new ALU)
//   val mem     = Module(new MEM)
//   val wb_pc   = Module(new WB_PC)
//   val wb_reg  = Module(new WB_REG)
//   val ebreak  = Module(new EBREAK)
//   val csr     = Module(new CSR)

//   val ctrl    = dec.io.ctrl
//   val mem_len = dec.io.mem_len
//   val load_U  = dec.io.load_U
//   val inst    = ifu.io.inst

//   // IF
//   val pc   = RegInit(UInt(32.W), "h80000000".U)
//   val snpc = pc + 4.U
//   pc := wb_pc.io.dnpc

//   ifu.io.pc := pc

//   // DE
//   dec.io.inst := ifu.io.inst

//   immgen.io.inst      := ifu.io.inst
//   immgen.io.inst_type := ctrl.inst_type

//   regfile.io.rs1_sel := dec.io.rs1
//   regfile.io.rs2_sel := dec.io.rs2
//   regfile.io.reg_we  := ctrl.reg_we
//   regfile.io.wr_sel  := dec.io.rd
//   regfile.io.wb_data := wb_reg.io.wb_data

//   io.rs1  := dec.io.rs1
//   io.rs2  := dec.io.rs2
//   io.rd   := dec.io.rd
//   io.jal  := dec.io.ctrl.jal
//   io.jalr := dec.io.ctrl.jalr
//   io.imm  := immgen.io.imm.asUInt;

//   // EX
//   val rs1     = regfile.io.rs1
//   val rs2     = regfile.io.rs2
//   val imm     = immgen.io.imm.asUInt
//   val alu_op1 = MuxLookup(ctrl.alu_sel1, 0.U)(Seq("b00".U -> rs1, "b01".U -> pc, "b10".U -> 0.U))
//   val alu_op2 = MuxLookup(ctrl.alu_sel2, 0.U)(Seq("b0".U -> rs2, "b1".U -> imm))

//   alu.io.op1      := alu_op1
//   alu.io.op2      := alu_op2
//   alu.io.cmp_U    := ctrl.cmp_U
//   alu.io.alu_func := ctrl.alu_func

//   // MEM
//   mem.io.addr   := alu.io.out
//   mem.io.len    := mem_len
//   mem.io.load_U := load_U
//   mem.io.mr     := ctrl.mr
//   mem.io.mw     := ctrl.mw
//   mem.io.data_w := rs2

//   // WB
//   csr.io.ren   := ctrl.csr.asBool & dec.io.rd =/= 0.U
//   csr.io.addr  := imm(11, 0)
//   csr.io.wen   := ctrl.csr.asBool & dec.io.rs1 =/= 0.U
//   csr.io.wtype := inst(13, 12)
//   csr.io.wdata := rs1
//   csr.io.ecall := ctrl.ecall.asBool
//   csr.io.pc    := pc

//   val target = Mux(ctrl.jalr.asBool, rs1, pc) + imm
//   wb_pc.io.mtvec       := csr.io.mtvec
//   wb_pc.io.ecall       := ctrl.ecall.asBool
//   wb_pc.io.mepc        := csr.io.mepc
//   wb_pc.io.mret        := ctrl.mret.asBool
//   wb_pc.io.target      := target
//   wb_pc.io.snpc        := snpc
//   wb_pc.io.jal         := ctrl.jal
//   wb_pc.io.jalr        := ctrl.jalr
//   wb_pc.io.take_branch := ctrl.branch & alu.io.cmp_out

//   wb_reg.io.alu    := alu.io.out
//   wb_reg.io.csr    := csr.io.rdata
//   wb_reg.io.mem    := mem.io.data_r
//   wb_reg.io.snpc   := snpc
//   wb_reg.io.wb_sel := ctrl.wb_sel

//   ebreak.io.ebreak := ctrl.ebreak
// }
