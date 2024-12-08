package utils

import chisel3._
import chisel3.util._

class CacheIO extends Bundle {
  val coreSide = Flipped(new CoreBusLite)
  val memSide = new CoreBus
}

class Cache extends CoreModule {
  val io = IO(new CacheIO)

   
}