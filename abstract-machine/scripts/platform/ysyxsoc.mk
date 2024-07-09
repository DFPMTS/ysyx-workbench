AM_SRCS := riscv/ysyxsoc/start.S \
           riscv/ysyxsoc/trm.c \
           riscv/ysyxsoc/cte.c \
           riscv/ysyxsoc/trap.S \
           platform/dummy/vme.c \
           platform/dummy/mpe.c \
					 riscv/ysyxsoc/ioe/ioe.c \
					 riscv/ysyxsoc/ioe/uart.c \
					 riscv/ysyxsoc/ioe/timer.c \
					 riscv/ysyxsoc/ioe/input.c \
					 riscv/ysyxsoc/ioe/gpu.c

CFLAGS    += -fdata-sections -ffunction-sections -Isrc/riscv/ysyxsoc
LDFLAGS   += -T $(AM_HOME)/scripts/ysyxsoc-linker.ld \
						 --defsym=_pmem_start=0x20000000 --defsym=_entry_offset=0x0
LDFLAGS   += --gc-sections -e _fsbl
CFLAGS += -DMAINARGS=\"$(mainargs)\"
.PHONY: $(AM_HOME)/am/src/riscv/ysyxsoc/trm.c

NPCFLAGS += $(IMAGE).bin
NPCFLAGS += -l $(shell dirname $(IMAGE).elf)/ysyxsoc-log.txt 
NPCFLAGS += -e $(IMAGE).elf

image: $(IMAGE).elf
	@$(OBJDUMP) -d $(IMAGE).elf > $(IMAGE).txt
	@echo + OBJCOPY "->" $(IMAGE_REL).bin
	@$(OBJCOPY) -S -O binary $(IMAGE).elf $(IMAGE).bin

run: image	
	$(MAKE) -C $(NPC_HOME) MAINARGS="$(NPCFLAGS)" sim