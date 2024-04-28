#include "ftrace.hpp"
#include "debug.hpp"
#include "func_sym.hpp"
#include "mem.hpp"
#include <cassert>

void ftrace_log(word_t pc, word_t dnpc, word_t inst, int rd, int rs1,
                word_t offset) {
  /*  only under these circumstances:
      [call]
      jal  x1,      offset
      jalr x1, rs1, offset
      [ret]
      jalr x0, x1,  0
  */
  static int level = 0;
  static char dst_addr[128];

  bool call = (rd == 1) ? true : false;
  bool ret = (rd == 0 && rs1 == 1 && offset == 0) ? true : false;

  // ignore non call/ret jal/jalr
  if (!call && !ret)
    return;

  Assert(!(call && ret), "Wrong logic for deciding call/ret");

  /*
   example:
   0x8000016c:                       call [f2@0x800000a4]
   0x800000f0:                         call [f1@0x8000005c]
   0x80000058:                         ret  [f0]
   0x80000100:                       ret  [f2]
   */

  // the @0x80000000 part
  if (call) {
    assert(sprintf(dst_addr, "@" FMT_WORD, dnpc));
  } else {
    dst_addr[0] = '\0';
  }

  // for call, func_name is func to jump to; for ret, func_name is func to
  // return from
  char *func_name = call ? func_sym_search(dnpc) : func_sym_search(pc);

  // indent level
  int indent = call ? (level++) * 2 : (--level) * 2;

  log_write(FMT_WORD ": %*s%s [%s%s]\n", pc, indent, "", call ? "call" : "ret ",
            func_name, dst_addr);
}