#ifndef __MEMORY_H__
#define __MEMORY_H__

#include <common.h>

#ifndef PGSIZE
#define PGSIZE 4096
#endif

#ifndef PGMASK
#define PGMASK (4096 - 1)
#endif


#define PG_ALIGN __attribute((aligned(PGSIZE)))

void* new_page(size_t);

#endif
