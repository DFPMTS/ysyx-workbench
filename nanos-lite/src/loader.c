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

void context_kload(PCB *pcb, void *entry, void *arg)
{
  kcontext((Area){pcb, &pcb[0] + 1}, entry, arg);
}

static void copy_ptr_str_to_stack(char *sp, char **write_ptr_p,
                                  int *str_offset_p, int count,
                                  char *const args[]) {

  char *write_ptr = *write_ptr_p;
  int str_offset = *str_offset_p;

  for (int i = 0; i < count; ++i) {
    // argv[i]
    *(char **)write_ptr = sp + str_offset;
    // copy str pointed by argv[i] to str area
    strcpy(sp + str_offset, args[i]);

    // incre offset & str_offset
    write_ptr += sizeof(char *);
    str_offset += strlen(args[i]) + 1;
  }
  // NULL-terminated
  *(char **)write_ptr = NULL;
  write_ptr += sizeof(char *);

  *write_ptr_p = write_ptr;
  *str_offset_p = str_offset;
}

void context_uload(PCB *pcb, const char *filename, char *const argv[],
                   char *const envp[]) {
  void *entry = (void *)loader(pcb, filename);
  Context *c = ucontext(NULL, (Area){pcb, &pcb[0] + 1}, entry);
  char *sp = heap.end;

  /* Calculate sp offset */
  int argc = 0;
  while (argv[argc]) {
    // count len of string table (argv part)
    sp -= strlen(argv[argc]) + 1;
    ++argc;
  }
  // argv[0] : file name
  ++argc;
  sp -= strlen(filename) + 1;

  int envc = 0;
  while (envp[envc]) {
    // count len of string table (envp part)
    sp -= strlen(envp[envc]) + 1;
    ++envc;
  }

  // string area offset (to sp)
  // the pointers are NULL-terminated, so +1 is needed for argc & envc
  // also need a sizeof(int) space for argc
  int str_offset = ((envc + 1) + (argc + 1)) * sizeof(char *) + sizeof(int);
  sp -= str_offset;

  /* Begin filling stack */
  char *write_ptr = sp;

  // argc
  *(int *)(write_ptr) = argc;
  write_ptr += sizeof(int);

  // argv[0]: filename
  *(char **)(write_ptr) = sp + str_offset;
  write_ptr += sizeof(char *);

  // argv[0] content
  strcpy(sp + str_offset, filename);
  str_offset += strlen(filename) + 1;

  // argv & argv's strings
  // filename has been handled, so argc - 1
  copy_ptr_str_to_stack(sp, &write_ptr, &str_offset, argc - 1, argv); 

  // envp & envp's strings
  copy_ptr_str_to_stack(sp, &write_ptr, &str_offset, envc, envp);

  c->gpr[8] = (uintptr_t)sp;

  assert(sp + str_offset == heap.end);
}
