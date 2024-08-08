#include <proc.h>

#define MAX_NR_PROC 4

static PCB pcb[MAX_NR_PROC] __attribute__((used)) = {};
static PCB pcb_boot = {};
PCB *current = NULL;

void naive_uload(PCB *pcb, const char *filename);

void switch_boot_pcb() {
  current = &pcb_boot;
}

void hello_fun(void *arg) {
  int j = 1;
  while (1) {
    Log("Hello World from Nanos-lite with arg '%s' for the %dth time!", (char *)arg, j);
    j ++;
    yield();
  }
}

void init_proc() {
  switch_boot_pcb();
  context_kload(&pcb[0], hello_fun, "Goodbye");
  context_kload(&pcb[1], hello_fun, "World");
  Log("Initializing processes...");

  // load program here
  // naive_uload(NULL, "/bin/nterm");
}

Context *schedule(Context *prev) {
  // update current PCB's cp
  current->cp = prev;
  // choose next PCB to run
  if (current == &pcb[0]) {
    current = &pcb[1];
  } else {
    current = &pcb[0];
  }
  return current->cp;
}

void context_kload(PCB *pcb, void *entry, void *arg)
{
  kcontext((Area){pcb, &pcb[0] + 1}, entry, arg);
}

static uintptr_t loader(PCB *pcb, const char *filename);

void context_uload(PCB *pcb, const char* filename){
}
