#include "itrace.hpp"
#include "disasm.hpp"

void itrace_generate(char *buf, word_t pc, word_t inst) {
  buf += snprintf(buf, 20, "0x%08x:", pc);

  buf += snprintf(buf, 20, "    %08x", inst);

  buf += snprintf(buf, 10, "    ");
  disassemble(buf, 128, pc, (uint8_t *)&inst, 4);
}