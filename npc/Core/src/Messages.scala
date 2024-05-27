import chisel3._
import chisel3.util._

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
