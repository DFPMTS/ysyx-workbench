NAME = fixedptc-test
SRCS = fixedptc-test.c

CFLAGS = -I$(NAVY_HOME)/libs/libfixedptc/include

fixedptc-test: fixedptc-test.c
	$(CC) $(CFLAGS) -o $@ $^

run: fixedptc-test
	./$^