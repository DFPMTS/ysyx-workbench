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

static void load_segment(PCB *pcb, int fd, Elf32_Phdr *phdr_p) {
  // round down p_offset & p_vaddr to page boundry
  uintptr_t offset = phdr_p->p_offset & ~PGMASK;
  uintptr_t vaddr =  phdr_p->p_vaddr & ~PGMASK;
  // end of this segment
  uintptr_t vaddr_mem_end = phdr_p->p_vaddr + phdr_p->p_memsz;
  uintptr_t vaddr_file_end = phdr_p->p_vaddr + phdr_p->p_filesz;
  // [vaddr_file_end, vaddr_mem_end) needs to be cleared
  uintptr_t vaddr_bss_ptr = vaddr_file_end;  

  // installing pages in unit of page
  for (; vaddr < vaddr_mem_end; vaddr += PGSIZE, offset += PGSIZE) {
    // allocate one page
    uintptr_t paddr = (uintptr_t)new_page(1);
    map(&pcb->as, (void *)vaddr, (void *)paddr, 0);
    fs_lseek(fd, offset, SEEK_SET);

    if (vaddr < vaddr_file_end) {
      // calculate read file size
      uintptr_t read_size = vaddr_file_end - vaddr;
      read_size = read_size > PGSIZE ? PGSIZE : read_size;
      // read into physical page
      fs_read(fd, (void *)paddr, read_size);
      // printf("load:  [%p, %p) <- [%p, %p)\n", vaddr, vaddr + read_size, paddr,
      //        paddr + read_size);
    }

    // clear bss section in this physical page
    uintptr_t vaddr_page_end = vaddr + PGSIZE;
    if(vaddr_page_end > vaddr_bss_ptr){
      // convert vaddr_bss_ptr to paddr
      uintptr_t paddr_bss_ptr = paddr | (vaddr_bss_ptr & PGMASK);
      // printf("clear: [%p, %p) <- [%p, %p)\n", vaddr_bss_ptr,vaddr_bss_ptr + vaddr_page_end - vaddr_bss_ptr, paddr_bss_ptr,paddr_bss_ptr + vaddr_page_end - vaddr_bss_ptr);
      memset((void *)paddr_bss_ptr, 0, vaddr_page_end - vaddr_bss_ptr);
      vaddr_bss_ptr = vaddr_page_end;
    }

    if(vaddr > pcb->max_brk){
      pcb->max_brk = vaddr;
    }
  }
}

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

  // clear max_brk, then load_segment will calculate it
  pcb->max_brk = 0;

  // program headers
  for (int phdr_i = 0; phdr_i < ehdr.e_phnum; ++phdr_i) {
    Elf32_Phdr phdr;
    fs_lseek(fd, ehdr.e_phoff + ehdr.e_phentsize * phdr_i, SEEK_SET);
    fs_read(fd, &phdr, ehdr.e_phentsize);
    if(phdr.p_type == PT_LOAD) {
      load_segment(pcb, fd, &phdr);      
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

/* context_uload
  return value: 0 if success
               <0 if failed
*/
int context_uload(PCB *pcb, const char *filename, char *const argv[],
                   char *const envp[]) {
  // printf("call context_uload: %s\n", filename);

  // try to open file
  if(fs_open(filename) < 0){
    return -2;
  }
  
  // user space
  protect(&pcb->as);

  // note that the argv/envp array and str they point to 
  // may be allocated on heap which may be overwrite,
  // so copy them before loading

  // install a 32KB stack
  #define STACK_NR_PAGES 8
  char *stack_bottom_paddr = new_page(STACK_NR_PAGES);
  char *stack_bottom_vaddr = (void *)(0x80000000 - STACK_NR_PAGES * PGSIZE);
  for (int i = 0; i < STACK_NR_PAGES; ++i) {
    map(&pcb->as, stack_bottom_vaddr + i * PGSIZE,
        stack_bottom_paddr + i * PGSIZE, 0);
  }

  // note that we are manipulating paddr stack (sp) directly
  char *sp = stack_bottom_paddr + STACK_NR_PAGES * PGSIZE;

  /* Calculate sp offset */
  int argc = 0;
  while (argv[argc]) {
    // count len of string table (argv part)
    sp -= strlen(argv[argc]) + 1;
    ++argc;
  }

  int envc = 0;
  while (envp[envc]) {
    // count len of string table (envp part)
    sp -= strlen(envp[envc]) + 1;
    ++envc;
  }

  // printf("uload\n");
  // printf("argc: %d\n", argc);
  // printf("argv: %p\n", argv);
  // for (int i = 0; i < argc; ++i) {
  //   printf("argv[%d]: %p - %s\n", i, argv[i], argv[i]);
  // }
  // for (int i = 0; envp[i]; ++i) {
  //   printf("envp[%d]: %s\n", i, envp[i]);
  // }

  // string area offset (to sp)
  // the pointers are NULL-terminated, so +1 is needed for argc & envc
  // also need a sizeof(int) space for argc
  int str_offset = ((envc + 1) + (argc + 1)) * sizeof(char *) + sizeof(int);
  // align to sizeof(uinptr_t)
  str_offset += (uintptr_t)sp % sizeof(uintptr_t);
  sp -= str_offset;

  /* Begin filling stack */
  char *write_ptr = sp;

  // argc
  *(int *)(write_ptr) = argc;
  write_ptr += sizeof(int);

  // argv & argv's strings
  copy_ptr_str_to_stack(sp, &write_ptr, &str_offset, argc, argv); 

  // envp & envp's strings
  copy_ptr_str_to_stack(sp, &write_ptr, &str_offset, envc, envp);

  // now we are safe to load new program
  void *entry = (void *)loader(pcb, filename);
  Context *c = ucontext(&pcb->as, (Area){pcb, &pcb[0] + 1}, entry);

  // we use sscratch to save user stack sp, and it should be vaddr of sp  
  c->sscratch = (uintptr_t)sp - (uintptr_t)stack_bottom_paddr +
                (uintptr_t)stack_bottom_vaddr;
  // printf("ret context_uload: sscratch 0x%x\n",c->sscratch);

  return 0;
}
