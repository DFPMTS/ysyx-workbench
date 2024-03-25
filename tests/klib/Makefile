IMPORTED_PATH = ./include
INC_PATH = ../sput-1.4.0 $(IMPORTED_PATH)
INC_FLAGS = $(addprefix -I,$(INC_PATH))
KLIB_SRC = $(AM_HOME)/klib/src
DST_DIR = ./build

define import
	
	echo "#include<stdarg.h>\n#include<stddef.h>\n" > $(IMPORTED_PATH)/klib_$(1).h
	sed -e '/^#/d' $(KLIB_SRC)/$(1).c >> $(IMPORTED_PATH)/klib_$(1).h	
endef


test : 
	mkdir -p $(DST_DIR)	
	mkdir -p ./include
	$(call import,string)
	$(call import,stdio)	
	gcc $(INC_FLAGS) test.c -o $(DST_DIR)/test
	$(DST_DIR)/test

clean: 
	-rm -rf $(DST_DIR)

.PHONT: test clean