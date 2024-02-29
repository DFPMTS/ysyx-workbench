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

#include <isa.h>
#include <cpu/cpu.h>
#include <readline/readline.h>
#include <readline/history.h>
#include "sdb.h"
#include "memory/vaddr.h"

static int is_batch_mode = false;

void init_regex();
void init_wp_pool();

/* We use the `readline' library to provide more flexibility to read from stdin. */
static char* rl_gets() {
  static char *line_read = NULL;

  if (line_read) {
    free(line_read);
    line_read = NULL;
  }

  line_read = readline("(nemu) ");

  if (line_read && *line_read) {
    add_history(line_read);
  }

  return line_read;
}

static int cmd_c(char *args) {
  cpu_exec(-1);
  return 0;
}


static int cmd_q(char *args) {
  return -1;
}

static int cmd_help(char *args);

static int cmd_si(char *args);

static int cmd_info(char *args);

static int cmd_x(char *args);

static struct {
  const char *name;
  const char *description;
  int (*handler) (char *);
} cmd_table [] = {
  { "help", "Display information about all supported commands", cmd_help },
  { "c", "Continue the execution of the program", cmd_c },
  { "q", "Exit NEMU", cmd_q },
  { "si", "Continue the execution of the program for N steps. When not given, N is default to 1.", cmd_si},
  { "info", "print information about registers or watchpoints.", cmd_info},
  { "x", "Examine memory.", cmd_x}

  /* TODO: Add more commands */

};

#define NR_CMD ARRLEN(cmd_table)

static int cmd_help(char *args) {
  /* extract the first argument */
  char *arg = strtok(NULL, " ");
  int i;

  if (arg == NULL) {
    /* no argument given */
    for (i = 0; i < NR_CMD; i ++) {
      printf("%s - %s\n", cmd_table[i].name, cmd_table[i].description);
    }
  }
  else {
    for (i = 0; i < NR_CMD; i ++) {
      if (strcmp(arg, cmd_table[i].name) == 0) {
        printf("%s - %s\n", cmd_table[i].name, cmd_table[i].description);
        return 0;
      }
    }
    printf("Unknown command '%s'\n", arg);
  }
  return 0;
}

static int cmd_si(char *args) {
  /* extract the first argument */
  char *arg = strtok(NULL, " ");

  if (arg == NULL) {
    // default value is 1
    cpu_exec(1);
  } else {
    // read argument
    int num_steps = atoi(args);

    if (num_steps <= 0) {
      // num_steps <= 0?
      printf("N have to a positive number\n");
    } else {
      // implicit conversion is ok here
      cpu_exec(num_steps);
    }
  }

  return 0;
}

static int cmd_info(char *args) {
  /* extract the first argument: r/w */
  char *arg = strtok(NULL, " ");

  if (arg == NULL) {
    // need argument
    printf("Need argument: r(registers) or w(watchpoints)\n");
  } else {
    if (strcmp("r", arg) == 0) {
      // print registers
      isa_reg_display();
    } else if (strcmp("w", arg) == 0) {
      // TODO print watch points
      TODO();
    }
  }

  return 0;
}

static int cmd_x(char *args) {
  // extract the first argument: N
  char *arg = strtok(NULL, " ");

  if (arg == NULL) {
    // need argument
    printf("Format: x N [expr]\n");
  } else {
    int N = atoi(arg);
    if (N > 0) {
      // extract the second argument.
      arg = strtok(NULL, " ");
      if (arg == NULL) {
        printf("Need expression.\n");
      } else {
        long signed_val = strtol(arg, NULL, 16);
        unsigned int addr = (uint32_t)signed_val;
        if (signed_val >= 0) {
          for (int i = 0; i < N; ++i) {
            printf("%0x08X:\t%0x08X\n", addr + 4 * i, vaddr_read(addr + 4 * i, 4));
          }
        } else {
          printf("[expr] has to be a valid address.\n");
        }
      }
      // TODO enable format other than hex number
    } else {
      printf("N has to be a positive number.\n");
    }
  }

  return 0;
}

void sdb_set_batch_mode() {
  is_batch_mode = true;
}

void sdb_mainloop() {
  if (is_batch_mode) {
    cmd_c(NULL);
    return;
  }

  for (char *str; (str = rl_gets()) != NULL; ) {
    char *str_end = str + strlen(str);

    /* extract the first token as the command */
    char *cmd = strtok(str, " ");
    if (cmd == NULL) { continue; }

    /* treat the remaining string as the arguments,
     * which may need further parsing
     */
    char *args = cmd + strlen(cmd) + 1;
    if (args >= str_end) {
      args = NULL;
    }

#ifdef CONFIG_DEVICE
    extern void sdl_clear_event_queue();
    sdl_clear_event_queue();
#endif

    int i;
    for (i = 0; i < NR_CMD; i ++) {
      if (strcmp(cmd, cmd_table[i].name) == 0) {
        if (cmd_table[i].handler(args) < 0) { return; }
        break;
      }
    }

    if (i == NR_CMD) { printf("Unknown command '%s'\n", cmd); }
  }
}

void init_sdb() {
  /* Compile the regular expressions. */
  init_regex();

  /* Initialize the watchpoint pool. */
  init_wp_pool();
}
