INC_DIR = $(abspath ./include)
VSRC_DIR = ./vsrc
CORE_SRC_DIR = ./Core/src

export PATH := $(PATH):$(abspath ./utils)

TOPNAME = top
VERILATOR = verilator
VERILATOR_CFLAGS += -MMD --build --cc \
					-O3 --x-assign fast --x-initial fast --noassert --trace
CXXFLAGS += -DTOP_NAME="\"V$(TOPNAME)\"" $(shell llvm-config --cxxflags) -fPIE -I$(INC_DIR)
LDFLAGS = $(shell llvm-config --libs)
					
BUILD_DIR = ./build
OBJ_DIR = $(BUILD_DIR)/obj_dir
BIN = $(BUILD_DIR)/$(TOPNAME)

$(shell mkdir -p $(BUILD_DIR))

CORE_SRCS = $(shell find $(abspath $(CORE_SRC_DIR)) -name "*.scala")
VSRCS = $(shell find $(abspath $(VSRC_DIR)) -name "*.v" -or -name "*.sv")
VSRCS_FLAG = $(VSRC_DIR)/.vsrc_flag
CSRCS = $(shell find $(abspath ./csrc) -name "*.c" -or -name "*.cc" -or -name "*.cpp")
CINCS = $(shell find $(abspath ./include) -name "*.h" -or -name "*.hpp")
test:
	mill -i __.test

gen_verilog: $(VSRCS_FLAG)

$(VSRCS_FLAG): $(CORE_SRCS)
	make verilog 
	touch $@

$(BIN): $(VSRCS_FLAG) $(CSRCS) $(CINCS)
	rm -rf $(OBJ_DIR)
	$(VERILATOR) $(VERILATOR_CFLAGS) \
		--top-module $(TOPNAME) $(VSRCS) $(CSRCS) \
		$(addprefix -CFLAGS , $(CXXFLAGS)) $(addprefix -LDFLAGS , $(LDFLAGS)) \
		--Mdir $(OBJ_DIR) --exe -o $(abspath $(BIN))

verilog:
	$(call git_commit, "generate verilog")	
	-rm -rf $(VSRC_DIR)
	mkdir -p $(VSRC_DIR)
	mill -i __.test.runMain Elaborate -td $(VSRC_DIR) --split-verilog

help:
	mill -i __.test.runMain Elaborate --help

compile:
	mill -i __.compile

bsp:
	mill -i mill.bsp.BSP/install

reformat:
	mill -i __.reformat

checkformat:
	mill -i __.checkFormat

clean:
	-rm -rf $(BUILD_DIR)

.PHONY: test verilog help compile bsp reformat checkformat clean gen_verilog

override MAINARGS += --diff /home/dfpmts/Documents/ysyx-workbench/nemu/build/riscv32-nemu-interpreter-so

# sim: $(BIN)
# 	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
# 	echo $(MAINARGS)
# 	$^ $(MAINARGS)

sim: 
	xmake build chisel
	xmake build Vtop
	xmake run Vtop $(MAINARGS)
	

xmake_test: 
	xmake build chisel
	xmake build Vtop
	xmake run Vtop --log log.txt --diff /home/dfpmts/Documents/ysyx-workbench/nemu/build/riscv32-nemu-interpreter-so

-include ../Makefile
