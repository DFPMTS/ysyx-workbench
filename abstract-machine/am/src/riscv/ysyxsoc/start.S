.section entry, "ax"
.globl _start
.type _start, @function

_start:
  mv s0, zero
  la sp, _stack_pointer

  la a0, _text_lma_start
  la a1, _text_lma_end
  la a2, _text_vma_start
  call _bootloader_copy  

  la a0, _data_lma_start
  la a1, _data_lma_end
  la a2, _data_vma_start
  call _bootloader_copy

  la a0, _rodata_lma_start
  la a1, _rodata_lma_end
  la a2, _rodata_vma_start
  call _bootloader_copy

  la a0, _bss_vma_start
  la a1, _bss_vma_end
  call _bootloader_clear

  call _trm_init

_bootloader_copy:
_bootloader_copy_loop:
  bge a0, a1, _bootloader_copy_done
  lw t0, 0(a0)
  sw t0, 0(a2)
  addi a0, a0, 4
  addi a2, a2, 4
  j _bootloader_copy_loop
_bootloader_copy_done:
  ret
  
_bootloader_clear:
_bootloader_clear_loop:
  bge a0, a1, _bootloader_clear_done
  sw zero, 0(a0)
  addi a0, a0, 4  
  j _bootloader_clear_loop
_bootloader_clear_done:
  ret
