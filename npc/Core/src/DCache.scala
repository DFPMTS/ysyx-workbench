import chisel3._
import chisel3.util._
import utils._
import dataclass.data

class DCacheIO extends CoreBundle {
  val IN_tagReq = Flipped(Decoupled(new DTagReq))
  val OUT_tagResp = new DTagResp
  val IN_dataReq = Flipped(Decoupled(new DDataReq))
  val OUT_dataResp = new DDataResp

  val IN_ctrlDataRead = Flipped(Valid(new DDataReq)) 
  val OUT_ctrlDataResp = new DDataResp
  val IN_ctrlDataWrite = Flipped(Valid(new DDataReq))
}

class DCache extends CoreModule {
  val io = IO(new DCacheIO)

  val tagArray = Module(new SRAMTemplate(DCACHE_SETS, DCACHE_TAG + 1, DCACHE_TAG + 1))  
  val dataArray = Module(new SRAMTemplate(DCACHE_SETS, CACHE_LINE * 8, 8))

  // * Src: 0 -> LSU, 1 -> Cache Controller

  // * tag rw 
  io.IN_tagReq.ready := true.B
  // * arbitration  
  tagArray.io.rw(io.IN_tagReq.bits.addr, io.IN_tagReq.bits.write, io.IN_tagReq.bits.write, io.IN_tagReq.bits.data.asUInt, io.IN_tagReq.valid)
  tagArray.io.r(0.U, false.B)
  // * resp
  io.OUT_tagResp := tagArray.io.rw.rdata.asTypeOf(new DTagResp)
  
  // * data r
  dataArray.io.r(io.IN_ctrlDataRead.bits.addr, io.IN_ctrlDataRead.valid)
  
  // * data rw
  io.IN_dataReq.ready := true.B
  dataArray.io.rw(io.IN_dataReq.bits.addr, io.IN_dataReq.bits.write, io.IN_dataReq.bits.wmask, io.IN_dataReq.bits.data, io.IN_dataReq.valid)
  when(io.IN_ctrlDataWrite.valid) {
    dataArray.io.rw(io.IN_ctrlDataWrite.bits.addr, io.IN_ctrlDataWrite.bits.write, io.IN_ctrlDataWrite.bits.wmask, io.IN_ctrlDataWrite.bits.data, io.IN_ctrlDataWrite.valid)
    io.IN_dataReq.ready := false.B
  }

  // * resp
  io.OUT_dataResp := dataArray.io.rw.rdata.asTypeOf(new DDataResp)
  io.OUT_ctrlDataResp := dataArray.io.r.rdata.asTypeOf(new DDataResp)
}