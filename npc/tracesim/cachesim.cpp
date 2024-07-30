#include <cassert>
#include <cmath>
#include <cstdint>
#include <cstdio>
#include <vector>

const int MAX_INDEX = 128;

#define INSDRAM(addr) (addr >= 0xa0000000 && addr < 0xa2000000)

enum {
  DirectMapped,
  FullyAssociative,
};

enum {
  FIFO,
  Random,
};

class Cache {
public:
  Cache(size_t num_cacheline, size_t cacheline_size, int config)
      : m_cacheline_size(cacheline_size), m_num_cacheline(num_cacheline),
        m_config(config), fifo_ptr(0) {
    // direct mapped
    printf("Cache (%lu lines) x (%lu bytes)\n", num_cacheline, cacheline_size);
    offset_width = log2(cacheline_size);
    index_width = log2(num_cacheline);
    assert(cacheline_size == (1 << offset_width));
    // assert(num_cacheline == (1 << index_width));
  }

  void FullyAssociativeAccess(size_t addr) {
    // size_t addr_index = (addr >> offset_width) & ((1 << index_width) - 1);
    size_t addr_tag = addr >> (offset_width);
    // printf("%x %x\n", addr, addr_tag);
    // for (int i = 0; i < m_num_cacheline; ++i) {
    //   printf("%x\n", tag[i]);
    // }
    ++num_access;
    bool miss = true;
    for (int i = 0; i < m_num_cacheline; ++i) {
      if (tag[i] == addr_tag && valid[i]) {
        miss = false;
        ref[i] = 1;
        break;
      }
    }
    if (miss) {
      ++num_miss;
      for (int i = 0; i < m_num_cacheline; ++i) {
        if (!valid[i]) {
          tag[i] = addr_tag;
          valid[i] = 1;
          // for (int j = 0; j < m_num_cacheline; ++j)
          //   ref[j] = i == j;

          return;
        }
      }
      if (replacement_policy == FIFO) {
        int replace = fifo_ptr;
        fifo_ptr = (fifo_ptr + 1) % m_num_cacheline;
        tag[replace] = addr_tag;
        valid[replace] = 1;
      } else {
        int replace = rand() % m_num_cacheline;
        tag[replace] = addr_tag;
        valid[replace] = 1;
      }
    }
  }

  void DirectMappedAccess(size_t addr) {
    size_t addr_index = (addr >> offset_width) & ((1 << index_width) - 1);
    size_t addr_tag = addr >> (offset_width + index_width);
    // printf("%x %x\n", addr, addr_tag);
    // for (int i = 0; i < m_num_cacheline; ++i) {
    //   printf("%x\n", tag[i]);
    // }
    ++num_access;
    if (tag[addr_index] != addr_tag || !valid[addr_index]) {
      ++num_miss;
      tag[addr_index] = addr_tag;
      valid[addr_index] = 1;
    }
  }

  void access(size_t addr) {
    if (m_config == DirectMapped) {
      DirectMappedAccess(addr);
    } else {
      FullyAssociativeAccess(addr);
    }
  }

  void print_stat() {
    const uint32_t abd = 5, c = 2;
    printf("%s\n",
           m_config == DirectMapped ? "Direct Mapped" : "Fully Associative");
    printf("Access: %lu, Miss: %lu\n", num_access, num_miss);
    printf("Miss rate: %.2lf%%\n", 100.0 * num_miss / num_access);
    printf("Miss penalty: %.2lf\n", (double)133 * num_miss / num_access);
    printf("Baseline: %.2lf\n", (double)(68.5));
    printf("\n");
  }
  int replacement_policy;

private:
  int fifo_ptr;

  int m_config;
  size_t m_num_cacheline;
  size_t m_cacheline_size;
  size_t offset_width;
  size_t index_width;
  size_t tag[MAX_INDEX] = {};
  bool valid[MAX_INDEX] = {};
  size_t ref[MAX_INDEX] = {};
  size_t num_access = 0;
  size_t num_miss = 0;
};

Cache icache(2, 16, DirectMapped);

std::vector<uint32_t> addr_buf;
size_t addr_buf_size;

void run_trace(Cache &icache) {
  bool start = false;
  uint64_t total_access = 0;
  for (size_t i = 0; i < addr_buf.size(); ++i) {
    if (!start && INSDRAM(addr_buf[i])) {
      start = true;
    }
    if (start) {
      total_access++;
      icache.access(addr_buf[i]);
    }
  }
  printf("Total access: %lu\n", total_access);
}

int main(int argc, char *argv[]) {
  assert(argc == 2);

  FILE *trace_fp = fopen(argv[1], "r");
  assert(trace_fp);

  uint32_t addr;
  int head = 100000000;
  while (fscanf(trace_fp, "%x", &addr) != EOF) {
    addr_buf.push_back(addr);
    // if (head-- < 0) {
    //   break;
    // }
    // if (addr_buf_size == MAX_TRACE_BUF) {
    //   run_trace(icache);
    //   addr_buf_size = 0;
    // }
  }

  // if (addr_buf_size > 0) {
  //   run_trace(icache);
  // }

  icache = Cache(1, 32, DirectMapped);
  run_trace(icache);
  icache.print_stat();

  icache = Cache(2, 16, DirectMapped);
  run_trace(icache);
  icache.print_stat();

  icache = Cache(2, 16, FullyAssociative);
  icache.replacement_policy = Random;
  run_trace(icache);
  icache.print_stat();

  icache = Cache(2, 16, FullyAssociative);
  icache.replacement_policy = FIFO;
  run_trace(icache);
  icache.print_stat();

  icache = Cache(3, 16, FullyAssociative);
  icache.replacement_policy = Random;
  run_trace(icache);
  icache.print_stat();

  icache = Cache(3, 16, FullyAssociative);
  icache.replacement_policy = FIFO;
  run_trace(icache);
  icache.print_stat();

  icache = Cache(4, 16, DirectMapped);
  run_trace(icache);
  icache.print_stat();

  icache = Cache(4, 16, FullyAssociative);
  icache.replacement_policy = Random;
  run_trace(icache);
  icache.print_stat();

  icache = Cache(4, 16, FullyAssociative);
  icache.replacement_policy = FIFO;
  run_trace(icache);
  icache.print_stat();

  icache = Cache(4, 32, DirectMapped);
  run_trace(icache);
  icache.print_stat();

  icache = Cache(4, 32, FullyAssociative);
  icache.replacement_policy = Random;
  run_trace(icache);
  icache.print_stat();

  icache = Cache(4, 32, FullyAssociative);
  icache.replacement_policy = FIFO;
  run_trace(icache);
  icache.print_stat();

  icache = Cache(8, 16, DirectMapped);
  run_trace(icache);
  icache.print_stat();

  icache = Cache(8, 16, FullyAssociative);
  icache.replacement_policy = Random;
  run_trace(icache);
  icache.print_stat();

  icache = Cache(8, 16, FullyAssociative);
  icache.replacement_policy = FIFO;
  run_trace(icache);
  icache.print_stat();

  icache = Cache(8, 16, DirectMapped);
  run_trace(icache);
  icache.print_stat();

  icache = Cache(8, 16, FullyAssociative);
  icache.replacement_policy = Random;
  run_trace(icache);
  icache.print_stat();

  icache = Cache(8, 16, FullyAssociative);
  icache.replacement_policy = FIFO;
  run_trace(icache);
  icache.print_stat();

  return 0;
}