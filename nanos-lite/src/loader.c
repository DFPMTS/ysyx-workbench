#include <proc.h>
#include <elf.h>
#include <fs.h>

#ifdef __LP64__
# define Elf_Ehdr Elf64_Ehdr
# define Elf_Phdr Elf64_Phdr
#else
# define Elf_Ehdr Elf32_Ehdr
# define Elf_Phdr Elf32_Phdr
#endif

#if defined(__ISA_AM_NATIVE__)
# define EXPECT_EM EM_X86_64
#elif defined(__ISA_RISCV32__)
# define EXPECT_EM EM_RISCV
#else
# error Unsupported ISA
#endif

size_t ramdisk_read(void *buf, size_t offset, size_t len);
size_t ramdisk_write(const void *buf, size_t offset, size_t len);

static uintptr_t loader(PCB *pcb, const char *filename) {
  int fd = fs_open(filename);
  assert(fd);

  Elf32_Ehdr ehdr;
  // ELF header
  fs_read(fd, &ehdr, sizeof(ehdr));

  // check ELF magic number
  assert(*(uint32_t*)&ehdr.e_ident == 0x464c457f);

  // check machine 
  assert(ehdr.e_machine == EXPECT_EM);

  // program headers
  for (int phdr_i = 0; phdr_i < ehdr.e_phnum; ++phdr_i) {
    Elf32_Phdr phdr;
    fs_lseek(fd, ehdr.e_phoff + ehdr.e_phentsize * phdr_i, SEEK_SET);
    fs_read(fd, &phdr, ehdr.e_phentsize);
    if(phdr.p_type == PT_LOAD) {
      fs_lseek(fd, phdr.p_offset, SEEK_SET);
      fs_read(fd, (void *)(uintptr_t)phdr.p_vaddr, phdr.p_filesz);
      memset((void *)(uintptr_t)(phdr.p_vaddr + phdr.p_filesz), 0,
             phdr.p_memsz - phdr.p_filesz);
    }
  }
  return (uintptr_t)(ehdr.e_entry);
}

void naive_uload(PCB *pcb, const char *filename) {
  uintptr_t entry = loader(pcb, filename);
  Log("Jump to entry = %p", entry);
  ((void(*)())entry) ();
}

