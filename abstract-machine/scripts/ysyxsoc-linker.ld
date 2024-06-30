ENTRY(_start)

MEMORY {
  mrom : ORIGIN = 0x20000000, LENGTH = 4k
  sram : ORIGIN = 0x0f000000, LENGTH = 8k
}

SECTIONS {
  /* _pmem_start and _entry_offset are defined in LDFLAGS */
  /* . = _pmem_start + _entry_offset; */
  . = ORIGIN(mrom);
  
  .text : {
    *(entry)
    *(.text*)
  }
  etext = .;
  
  _etext = .;
  .rodata : {
    *(.rodata*)
    *(.srodata*)
  }

  .data : {
    *(.data*)
    *(.sdata*)
  }

  edata = .;
  _data = .;
  .bss : {
	_bss_start = .;
    *(.bss*)
    *(.sbss*)
    *(.scommon)
  }

  . = ORIGIN(sram);

  _stack_top = ALIGN(0x1000);
  . = _stack_top + LENGTH(sram)/2;
  _stack_pointer = .;
  end = .;
  _end = .;
  _heap_start = ALIGN(0x1000);
}
