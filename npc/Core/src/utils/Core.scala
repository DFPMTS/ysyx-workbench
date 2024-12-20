package utils

import chisel3._
import chisel3.util._
import _empty_.HasDecodeConfig
import _empty_.HasDecodeConfig

trait HasDecodeConfig {
  def FuTypeWidth = 4
  def OpcodeWidth = 4
  def ImmTypeWidth = 4
}

trait HasCoreParameters {
  def XLEN = 32
  def IDLEN = 4  

  // * Issue Width
  def ISSUE_WIDTH = 1
  def MACHINE_WIDTH = 4
  def COMMIT_WIDTH = 2


  // * Physical Register
  def NUM_PREG = 64
  def PREG_IDX_W = log2Up(NUM_PREG).W

  // * ROB 
  def ROB_SIZE = 64
  def ROB_IDX_W = log2Up(ROB_SIZE).W

  // * Load Queue
  def LDQ_SIZE = 16  
  def LDQ_IDX_W = log2Up(LDQ_SIZE).W

  // * Store Queue
  def STQ_SIZE = 16
  def STQ_IDX_W = log2Up(STQ_SIZE).W

  // * Flag
  def FLAG_W = 4.W


  def ZERO = 0.U
}

trait CoreModule extends Module with HasCoreParameters with HasDecodeConfig

trait CoreBundle extends Bundle with HasCoreParameters with HasDecodeConfig