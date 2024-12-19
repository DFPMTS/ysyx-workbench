package utils

import chisel3._
import chisel3.util._

class RingBufferPtr(size: Int) extends CoreBundle {  
  // * order matters
  // ** extra flag bit, used for distinguishing between full and empty
  // ** everytime we wrap around, we flip this bit
  val WIDTH_INDEX = log2Up(size)
  val flag = UInt()  
  // ** real index
  val index = UInt(WIDTH_INDEX.W)
  
  def this(size: Int, flag: UInt, index: UInt) = {
    this(size)
    this.flag := flag
    this.index := index
  }

  def +(value: UInt): RingBufferPtr = {
    val nextPtr = Wire(new RingBufferPtr(size))
    if(isPow2(size)) {
      // * if size is power of two, "+" operation automatically wraps around
      nextPtr := (Cat(this.flag, this.index) + value).asTypeOf(nextPtr)
    } else {
      // * not power of two, then we have to subtract size for index
      // ** rawNextIndex is INDEX_W+1 bits
      val rawNextIndex = this.index +& value
      // ** subtract
      val diff = rawNextIndex - size.U((WIDTH_INDEX + 1).W)
      when (~diff(WIDTH_INDEX)) {
        // *** diff positive, wraps around
        nextPtr.flag := ~this.flag
        nextPtr.index := diff(WIDTH_INDEX - 1, 0)
      } .otherwise {
        nextPtr.flag := this.flag
        nextPtr.index := rawNextIndex
      }
    }
    nextPtr
  }

  def -(value: UInt): RingBufferPtr = {
    val nextPtr = this + (size.U - value)
    nextPtr
  }
}