#include "utils.h"
#include <assert.h>
#include <elf.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define ELF_BUF_SIZE 32 * 1024 * 1024

static char buf[ELF_BUF_SIZE + 10];

static Elf32_Ehdr elf_hdr;
static Elf32_Shdr shstrtab_hdr;
static Elf32_Shdr strtab_hdr;
static Elf32_Shdr symtab_hdr;

static char *get_section_name(Elf32_Shdr *shdr) {
  return buf + shstrtab_hdr.sh_offset + shdr->sh_name;
}

static bool section_name_eq(Elf32_Shdr *shdr, char *s) {
  return strcmp(get_section_name(shdr), s) == 0;
}

static char *get_symbol_name(Elf32_Sym *sym) {
  return buf + strtab_hdr.sh_offset + sym->st_name;
}

typedef struct func_sym {
  char *name;
  word_t offset;
  word_t size;
  struct func_sym *next;
} func_sym;

static func_sym *head = NULL;

static void func_sym_push(Elf32_Sym *sym) {
  func_sym *entry = malloc(sizeof(func_sym));
  entry->name = get_symbol_name(sym);
  entry->offset = sym->st_value;
  entry->size = sym->st_size;
  entry->next = head;
  head = entry;
}

// * ??? when not found
char *func_sym_search(word_t pc) {
  func_sym *func = head;
  while (func) {
    if (pc >= func->offset && pc < func->offset + func->size) {
      return func->name;
    }
    func = func->next;
  }
  return "???";
}

void init_func_sym(char *elf_file) {
  if (!elf_file) {
    Log("ELF not provided.");
    return;
  }

  FILE *fp = fopen(elf_file, "rb");
  Assert(fp, "Can not open ELF: '%s'", elf_file);

  fseek(fp, 0, SEEK_END);
  long size = ftell(fp);
  Log("The ELF is %s, size = %ld", elf_file, size);
  if(size > ELF_BUF_SIZE){
    panic("ELF buffer too small. Current size: %u\n",ELF_BUF_SIZE);
  }

  fseek(fp, 0, SEEK_SET);
  int ret = fread(buf, size, 1, fp);
  assert(ret == 1);

  fclose(fp);

  // * read elf header
  memcpy(&elf_hdr, buf, sizeof(elf_hdr));

  // * index of section name table in section header table
  word_t shstrtab_idx = elf_hdr.e_shstrndx;
  // * size of each entry in section header table
  word_t sh_ent_size = elf_hdr.e_shentsize;
  // * offset of section header table in file
  word_t sh_offset = elf_hdr.e_shoff;
  // * number of entries in section header table
  word_t sh_num = elf_hdr.e_shnum;

  // * find the section name table
  memcpy(&shstrtab_hdr, buf + sh_offset + sh_ent_size * shstrtab_idx,
         sizeof(Elf32_Shdr));
  assert(shstrtab_hdr.sh_type == SHT_STRTAB);
  assert(section_name_eq(&shstrtab_hdr, ".shstrtab"));

  // * search for .strtab .symtab section headers
  Elf32_Shdr sh;
  for (int i = 0; i < sh_num; ++i) {
    memcpy(&sh, buf + sh_offset + sh_ent_size * i, sizeof(Elf32_Shdr));
    if (sh.sh_type == SHT_STRTAB && section_name_eq(&sh, ".strtab")) {
      // * found strtab
      memcpy(&strtab_hdr, &sh, sizeof(Elf32_Shdr));
    }
    if (sh.sh_type == SHT_SYMTAB && section_name_eq(&sh, ".symtab")) {
      // * found symtab
      memcpy(&symtab_hdr, &sh, sizeof(Elf32_Shdr));
    }
  }

  // * strtab
  assert(strtab_hdr.sh_type == SHT_STRTAB);
  assert(section_name_eq(&strtab_hdr, ".strtab"));

  // * symtab
  assert(symtab_hdr.sh_type == SHT_SYMTAB);
  assert(section_name_eq(&symtab_hdr, ".symtab"));

  // * func syms
  Elf32_Sym sym;
  word_t sym_num = symtab_hdr.sh_size / symtab_hdr.sh_entsize;
  for (int i = 1; i < sym_num; ++i) {
    memcpy(&sym, buf + symtab_hdr.sh_offset + symtab_hdr.sh_entsize * i,
           sizeof(Elf32_Sym));
    // * only consider functions
    if (ELF32_ST_TYPE(sym.st_info) == STT_FUNC) {
      func_sym_push(&sym);
    }
  }
}
