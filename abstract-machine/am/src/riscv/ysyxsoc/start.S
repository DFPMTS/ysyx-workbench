.section entry, "ax"
.globl _start
.type _start, @function

_start:
  mv s0, zero
  la sp, _stack_pointer

  call _trm_init

.section fsbl, "ax"
.global _fsbl
.type _fsbl, @function

_fsbl:
  la a0, _ssbl_lma_start
  la a1, _ssbl_lma_end
  la a2, _ssbl_vma_start
_fsbl_copy_loop:
  bge a0, a1, _fsbl_copy_done
  lw t0, 0(a0)
  sw t0, 0(a2)
  addi a0, a0, 4
  addi a2, a2, 4
  j _fsbl_copy_loop
_fsbl_copy_done:
  call _ssbl

.section ssbl, "ax"
.global _ssbl
.type _ssbl, @function
_ssbl:
  la a0, _text_lma_start
  la a1, _text_lma_end
  la a2, _text_vma_start
  call _ssbl_copy  

  la a0, _data_lma_start
  la a1, _data_lma_end
  la a2, _data_vma_start
  call _ssbl_copy

  la a0, _rodata_lma_start
  la a1, _rodata_lma_end
  la a2, _rodata_vma_start
  call _ssbl_copy

  la a0, _bss_vma_start
  la a1, _bss_vma_end
  call _ssbl_clear
  call _start

_ssbl_copy:
_ssbl_copy_loop:
  bge a0, a1, _ssbl_copy_done
  lw t0, 0(a0)
  sw t0, 0(a2)
  addi a0, a0, 4
  addi a2, a2, 4
  j _ssbl_copy_loop
_ssbl_copy_done:
  ret
  
_ssbl_clear:
_ssbl_clear_loop:
  bge a0, a1, _ssbl_clear_done
  sw zero, 0(a0)
  addi a0, a0, 4  
  j _ssbl_clear_loop
_ssbl_clear_done:
  ret
