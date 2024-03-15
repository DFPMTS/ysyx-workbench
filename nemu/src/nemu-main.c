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

#include <common.h>
#include <stdio.h>

void init_monitor(int, char *[]);
void am_init_monitor();
void engine_start();
int is_exit_status_bad();
word_t expr(char *e, bool *success);

static char buf [65536+10];

int main(int argc, char *argv[]) {
  char *test_expr_path;
  /* Initialize the monitor. */
#ifdef CONFIG_TARGET_AM
  am_init_monitor();
#else
  init_monitor(argc, argv);
#endif

  if ((test_expr_path = getenv("TEST_EXPR"))) {
    // format: [ref]  [expr]
    uint32_t ref;
    Log("Reading input from: %s\n",test_expr_path);
    FILE *test_input = fopen(test_expr_path, "r");
    Assert(test_input, "Failed to open file");
    Assert(fscanf(test_input, "%u", &ref), "Read reference output failed");
    Assert(fgets(buf, 65536, test_input), "Read expr failed");    
    bool success;
    uint32_t expr_val = expr(buf, &success);
    Log("expr value: %u\n", expr_val);
    Assert(success, "Eval failed.");
    Assert(ref == expr_val, "Wrong answer");
  } else {

    /* Start engine. */
    engine_start();

    return is_exit_status_bad();
  }
}
