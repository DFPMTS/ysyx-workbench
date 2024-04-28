#ifndef FTRACE_HPP
#define FTRACE_HPP

#include "mem.hpp"

void ftrace_log(word_t pc, word_t dnpc, word_t inst, int rd, int rs1,
                word_t offset);
#endif