ENTRY(_start)

MEMORY {
  mrom  : ORIGIN = 0x20000000, LENGTH = 4k
  sram  : ORIGIN = 0x0f000000, LENGTH = 8k
  flash : ORIGIN = 0x30000000, LENGTH = 1M
}

SECTIONS {
  /* _pmem_start and _entry_offset are defined in LDFLAGS */
  /* . = _pmem_start + _entry_offset; */  
  
  .text : ALIGN(8){
    *(entry)
    *(.text*)
  } > flash
  
  .rodata : ALIGN(8){
    *(.rodata*)
    *(.srodata*)
  } > flash
  
  .data : ALIGN(8){
    *(.data*)
    *(.sdata*)
  } > sram AT> flash

  .bss (NOLOAD) : ALIGN(8){
    *(.bss*)
    *(.sbss*)
    *(.scommon)
  } > sram

  .heap (NOLOAD) : ALIGN(8){
    _heap_start = .;
    . = _heap_start + 0x800;
    _heap_end = .;
  } > sram

  /* _heap_start = ALIGN(8);
  . = _heap_start + 0x800; 
  _heap_end = .; */

  .stack (NOLOAD) : ALIGN(8){
    _stack_top = .;
    . = _stack_top + 0x800;
    _stack_pointer = .;
  } > sram

  /* _stack_top = ALIGN(8);
  . = _stack_top + 0x800;
  _stack_pointer = .; */
  end = .;
  _end = .;
}

_data_lma_start = LOADADDR(.data);
_data_lma_end = _data_lma_start + SIZEOF(.data);
_data_vma_start = ADDR(.data);
_data_vma_end = _data_vma_start + SIZEOF(.data);
  
_bss_start = ADDR(.bss);
_bss_end = _bss_start + SIZEOF(.bss);

