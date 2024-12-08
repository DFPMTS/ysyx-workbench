package utils

import chisel3._
import chisel3.util._

trait HasCoreParameters {
  def XLEN = 32
  def IDLEN = 4  
}

trait CoreModule extends Module with HasCoreParameters

trait CoreBundle extends Bundle with HasCoreParameters