#include <common.h>
#include <proc.h>

Context* schedule(Context *prev);
void do_syscall(Context *c);

__attribute__((unused)) static void print_context(Context *c)
{
  printf("do_event return Context:\n");
  printf("current: %p\n", current);
  printf("sp:       0x%x\n",c->gpr[2]);
  printf("sscratch: 0x%x\n",c->sscratch);
  printf("\n");
}

static Context* do_event(Event e, Context* c) {
  asm volatile("csrw sscratch, zero");
  Context *ret_ctx = c;
  switch (e.event) {
    case EVENT_YIELD:
      // Log("EVENT YIELD");
      ret_ctx = schedule(c);
      break;
    case EVENT_SYSCALL:
      // Log("EVENT SYSCALL");
      do_syscall(c);
      break;
    default: panic("Unhandled event ID = %d", e.event);
  }

  return ret_ctx;
}

void init_irq(void) {
  Log("Initializing interrupt/exception handler...");
  cte_init(do_event);
}
