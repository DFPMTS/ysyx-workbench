/***************************************************************************************
* Copyright (c) 2014-2022 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

#include "sdb.h"
#include <cpu/cpu.h>

#define NR_WP 3

typedef struct watchpoint {
  int NO;
  struct watchpoint *next;
  word_t last_value;
  char *expr;
  /* TODO: Add more members if necessary */

} WP;

static WP wp_pool[NR_WP] = {};
static WP *head = NULL, *free_ = NULL, *free_last = NULL;
static int next_NO = NR_WP; 

void init_wp_pool() {
  int i;
  for (i = 0; i < NR_WP; i ++) {
    wp_pool[i].NO = i;
    wp_pool[i].next = (i == NR_WP - 1 ? NULL : &wp_pool[i + 1]);
  }

  head = NULL;
  free_ = wp_pool;
  free_last = &wp_pool[NR_WP - 1];
}

/* TODO: Implement the functionality of watchpoint */

void wp_check(vaddr_t pc) {  
  WP *cur = head;
  bool changed = false;
  while (cur) {
    bool success = true;
    word_t new_val = expr(cur->expr, &success);
    if (!success) {
      Log("Invalid expr.");
    } else {
      if (new_val != cur->last_value) {
        // stop
        printf("Watch point [%d] triggered on 0x%X, value: %u\n", cur->NO, pc,
               new_val);
        cur->last_value = new_val;
        changed = true;
      }
    }
    cur = cur->next;
  }

  if (changed) {
    set_nemu_state(NEMU_STOP, pc, -1);
  }
}

void wp_add(char *s)
{  
  if(free_){
    bool success = true;    
    free_->last_value = expr(s, &success);
    if(!success){
      printf("Invalid expr.\n");      
      return;
    }
    int len = strlen(s) + 1;
    free_->expr = malloc(len);
    memcpy(free_->expr, s, len);
    WP *next_free = free_->next;
    free_->next = head;
    head = free_;
    free_ = next_free;
    if(!free_){
      // running out of free watch points
      free_last = NULL;
    }
  }else{
    printf("Too many watch points!\n");
  }
}

void wp_display() {
  WP *cur = head;
  while (cur) {
    bool success = true;
    word_t val = expr(cur->expr, &success);
    printf("Watch point [%d]: expr=%s, value=", cur->NO, cur->expr);    
    if (!success) {
      printf("Not Available\n") ;
    } else {
      printf("%u\n",val);
    }
    cur = cur->next;
  }
}

void wp_delete(int NO)
{
  WP *cur = head;
  WP *prev = NULL;
  while (cur) {
    if(cur->NO == NO){
      // found
      if(prev){        
        prev->next = cur->next;
      }else{
        // prev not exist, we are deleting head
        head = cur->next;
      }
      cur->next = NULL;
      cur->NO = next_NO++;
      free(cur->expr);
      if (free_) {        
        free_last->next = cur;        
      }else{
        free_ = cur;
        free_last = cur;
      }
      printf("Watch point [%d] deleted.\n", NO);
    }
    prev = cur;
    cur = cur->next;
  }
}