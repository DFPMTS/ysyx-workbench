ENTRY(_fsbl)

MEMORY {
  mrom  : ORIGIN = 0x20000000, LENGTH = 4k
  sram  : ORIGIN = 0x0f000000, LENGTH = 8k
  flash : ORIGIN = 0x30000000, LENGTH = 8M
  psram : ORIGIN = 0x80000000, LENGTH = 4M
}

SECTIONS {
  .fsbl : ALIGN(8){
    *(fsbl)
  } > flash

  .ssbl : ALIGN(8){
    *(ssbl)
  } > sram AT> flash

  .text : ALIGN(8){  
    *(entry)
    *(.text*)
  } > psram AT> flash
  
  .rodata : ALIGN(8){
    *(.rodata*)
    *(.srodata*)
  } > psram AT> flash
  
  .data : ALIGN(8){
    *(.data*)
    *(.sdata*)
  } > psram AT> flash

  .bss (NOLOAD) : ALIGN(8){
    *(.bss*)
    *(.sbss*)
    *(.scommon)
  } > psram

  .heap (NOLOAD) : ALIGN(8){
    _heap_start = .;
    . = _heap_start + 0x100000;
    _heap_end = .;
  } > psram

  .stack (NOLOAD) : ALIGN(8){
    _stack_top = .;
    . = _stack_top + 0x1000;
    _stack_pointer = .;
  } > sram

  end = .;
  _end = .;
}

_ssbl_lma_start = LOADADDR(.ssbl);
_ssbl_lma_end = _ssbl_lma_start + SIZEOF(.ssbl);
_ssbl_vma_start = ADDR(.ssbl);
_ssbl_vma_end = _ssbl_vma_start + SIZEOF(.ssbl);

_text_lma_start = LOADADDR(.text);
_text_lma_end = _text_lma_start + SIZEOF(.text);
_text_vma_start = ADDR(.text);
_text_vma_end = _text_vma_start + SIZEOF(.text);

_rodata_lma_start = LOADADDR(.rodata);
_rodata_lma_end = _rodata_lma_start + SIZEOF(.rodata);
_rodata_vma_start = ADDR(.rodata);
_rodata_vma_end = _rodata_vma_start + SIZEOF(.rodata);

_data_lma_start = LOADADDR(.data);
_data_lma_end = _data_lma_start + SIZEOF(.data);
_data_vma_start = ADDR(.data);
_data_vma_end = _data_vma_start + SIZEOF(.data);
  
_bss_vma_start = ADDR(.bss);
_bss_vma_end = _bss_vma_start + SIZEOF(.bss);

