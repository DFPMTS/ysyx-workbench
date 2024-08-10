#include <memory.h>

static void *pf = NULL;

void* new_page(size_t nr_page) {
  // heap_ptr: point to end of heap
  // allocate: first round down heap_ptr to PAGE_SZ
  //           then return heap_ptr - nr_page * PAGE_SZ

  static uint32_t heap_ptr = -1;
  // initialize heap_ptr to heap.end
  if (heap_ptr == -1) {
    heap_ptr = (uint32_t)heap.end;
  }

  // move to page boundry
  heap_ptr &= ~PGMASK;
  // decre by nr_page * PGSIZE
  heap_ptr -= nr_page * PGSIZE;

  return (void *)heap_ptr;
}

#ifdef HAS_VME
static void *pg_alloc(int n) {
  void *ptr = new_page(n / PGSIZE);
  memset(ptr, 0, n);
  return ptr;
}
#endif

void free_page(void *p) {
  panic("not implement yet");
}

/* The brk() system call handler. */
int mm_brk(uintptr_t brk) {
  return 0;
}

void init_mm() {
  pf = (void *)ROUNDUP(heap.start, PGSIZE);
  Log("free physical pages starting from %p", pf);

#ifdef HAS_VME
  vme_init(pg_alloc, free_page);
#endif
}
