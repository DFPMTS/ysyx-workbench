import chisel3._
import chisel3.util._
import chisel3.experimental.dataview._

class PC_Message extends Bundle {
  val pc = UInt(32.W)
}

class IFU_Message extends Bundle {
  val pc   = UInt(32.W)
  val inst = UInt(32.W)
}

class IDU_Out extends Bundle {
  val pc   = UInt(32.W)
  val inst = UInt(32.W)
  val imm  = UInt(32.W)
  val ctrl = new Control
}

class IDU_Message extends Bundle {
  val pc   = UInt(32.W)
  val inst = UInt(32.W)
  val imm  = UInt(32.W)
  val ctrl = new Control
  val rs1  = UInt(32.W)
  val rs2  = UInt(32.W)
}

class EXU_Message extends Bundle {
  val pc          = UInt(32.W)
  val inst        = UInt(32.W)
  val imm         = UInt(32.W)
  val ctrl        = new Control
  val rs1         = UInt(32.W)
  val rs2         = UInt(32.W)
  val alu_out     = UInt(32.W)
  val alu_cmp_out = Bool()
  val mem_out     = UInt(32.W)
}

class WBU_Message extends Bundle {
  val pc      = UInt(32.W)
  val inst    = UInt(32.W)
  val dnpc    = UInt(32.W)
  val wb_data = UInt(32.W)
  val reg_we  = Bool()
  val ebreak  = Bool()
}

class AXI_Lite extends Bundle {
  val ar = Decoupled(new Bundle {
    val addr = UInt(32.W)
  })

  val r = Flipped(Decoupled(new Bundle {
    val data = UInt(32.W)
    val resp = UInt(2.W)
  }))

  val aw = Decoupled(new Bundle {
    val addr = UInt(32.W)
  })

  val w = Decoupled(new Bundle {
    val data = UInt(32.W)
    val strb = UInt(4.W)
  })

  val b = Flipped(Decoupled(new Bundle {
    val resp = UInt(2.W)
  }))
}

class AXI4(val dataBits: Int, val addrBits: Int) extends Bundle {

  val aw = Decoupled(new Bundle {
    val addr  = UInt(addrBits.W)
    val id    = UInt(4.W) // tied to LOW
    val len   = UInt(8.W) // tied to 0b00
    val size  = UInt(3.W)
    val burst = UInt(2.W) // tied to 0b01
  })

  val w = Decoupled(new Bundle {
    val data = UInt(dataBits.W)
    val strb = UInt((dataBits / 8).W)
    val last = Bool()
  })

  val b = Flipped(Decoupled(new Bundle {
    val resp = UInt(2.W)
    val id   = UInt(4.W) // tied to LOW
  }))

  val ar = Decoupled(new Bundle {
    val addr  = UInt(dataBits.W)
    val id    = UInt(4.W) // tied to LOW
    val len   = UInt(8.W) // tied to 0b00
    val size  = UInt(3.W)
    val burst = UInt(2.W) // tied to 0b01
  })

  val r = Flipped(Decoupled(new Bundle {
    val resp = UInt(2.W)
    val data = UInt(dataBits.W)
    val last = Bool()
    val id   = UInt(4.W) // tied to LOW
  }))

}

class AXI4ysyxSoC(val dataBits: Int, val addrBits: Int) extends Bundle {
  val awready = Input(Bool())
  val awvalid = Output(Bool())
  val awaddr  = Output(UInt(addrBits.W))
  val awid    = Output(UInt(4.W))
  val awlen   = Output(UInt(8.W))
  val awsize  = Output(UInt(3.W))
  val awburst = Output(UInt(2.W))
  val wready  = Input(Bool())
  val wvalid  = Output(Bool())
  val wdata   = Output(UInt(dataBits.W))
  val wstrb   = Output(UInt((dataBits / 8).W))
  val wlast   = Output(Bool())
  val bready  = Output(Bool())
  val bvalid  = Input(Bool())
  val bresp   = Input(UInt(2.W))
  val bid     = Input(UInt(4.W))
  val arready = Input(Bool())
  val arvalid = Output(Bool())
  val araddr  = Output(UInt(addrBits.W))
  val arid    = Output(UInt(4.W))
  val arlen   = Output(UInt(8.W))
  val arsize  = Output(UInt(3.W))
  val arburst = Output(UInt(2.W))
  val rready  = Output(Bool())
  val rvalid  = Input(Bool())
  val rresp   = Input(UInt(2.W))
  val rdata   = Input(UInt(dataBits.W))
  val rlast   = Input(Bool())
  val rid     = Input(UInt(4.W))
}

object AXI4 {
  implicit val axiView = DataView[AXI4ysyxSoC, AXI4](
    vab => new AXI4(vab.dataBits, vab.addrBits),
    _.awready -> _.aw.ready,
    _.awvalid -> _.aw.valid,
    _.awaddr -> _.aw.bits.addr,
    _.awid -> _.aw.bits.id,
    _.awlen -> _.aw.bits.len,
    _.awsize -> _.aw.bits.size,
    _.awburst -> _.aw.bits.burst,
    _.wready -> _.w.ready,
    _.wvalid -> _.w.valid,
    _.wdata -> _.w.bits.data,
    _.wstrb -> _.w.bits.strb,
    _.wlast -> _.w.bits.last,
    _.bready -> _.b.ready,
    _.bvalid -> _.b.valid,
    _.bresp -> _.b.bits.resp,
    _.bid -> _.b.bits.id,
    _.arready -> _.ar.ready,
    _.arvalid -> _.ar.valid,
    _.araddr -> _.ar.bits.addr,
    _.arid -> _.ar.bits.id,
    _.arlen -> _.ar.bits.len,
    _.arsize -> _.ar.bits.size,
    _.arburst -> _.ar.bits.burst,
    _.rready -> _.r.ready,
    _.rvalid -> _.r.valid,
    _.rresp -> _.r.bits.resp,
    _.rdata -> _.r.bits.data,
    _.rlast -> _.r.bits.last,
    _.rid -> _.r.bits.id
  )

  implicit val axiView2 = axiView.invert(ab => new AXI4ysyxSoC(ab.dataBits, ab.addrBits))
}
