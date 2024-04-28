#ifndef FUNC_SYM_HPP
#define FUNC_SYM_HPP

#include "mem.hpp"

char *func_sym_search(word_t pc);
void init_func_sym(const char *elf_file);

#endif