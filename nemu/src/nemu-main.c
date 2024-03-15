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

int main(int argc, char *argv[]) {
  char *test_expr_path;
  if ((test_expr_path = getenv("TEST_EXPR"))) {
    // format: [ref]  [expr]
    uint32_t ref;
    char *buf = malloc(65536);
    printf("%s\n",test_expr_path);
    FILE *test_input = fopen(test_expr_path, "r");
    Assert(test_input, "Failed to open file");
    Assert(fgets(buf, 65536, test_input), "Read expr failed");    
    Log("%s\n",buf);
    Assert(fscanf(test_input, "%u", &ref), "Read reference output failed");
    Log("%u\n",ref);    
    bool success;
    printf("expr value: %u\n", expr(buf, &success));
    Assert(success, "Eval failed.");
  } else {
    /* Initialize the monitor. */
#ifdef CONFIG_TARGET_AM
    am_init_monitor();
#else
    init_monitor(argc, argv);
#endif

    /* Start engine. */
    engine_start();

    return is_exit_status_bad();
  }
}
