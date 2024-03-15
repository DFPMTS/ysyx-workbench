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

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <assert.h>
#include <string.h>

#define ARRLEN(x) (sizeof(x)/sizeof(x[0]))

// this should be enough
static char buf[65536] = {};
static char code_buf[65536 + 128] = {}; // a little larger than `buf`
static char *code_format =
"#include <stdio.h>\n"
"int main() { "
"  unsigned result = %s; "
"  printf(\"%%u\", result); "
"  return 0; "
"}";

static char *gen_rand_decimal_uint32() {
  uint32_t num = (uint32_t)((uint64_t)rand() * (uint64_t)rand()) % UINT32_MAX;

  char *buf = malloc(20);

  if (rand() & 1) {
    snprintf(buf, 20, "%uu", num);
  } else {
    snprintf(buf, 20, "0x%xu", num);
  }
  return buf;
}

static char *gen_rand_from_list(char *list[], int list_len) 
{
  int id = rand() % list_len;
  int len = strlen(list[id]) + 1;
  char *buf = malloc(len);
  snprintf(buf, len, "%s", list[id]);
  return buf;
}

static char *gen_rand_binary_op() {
  static char *binary_ops[] = {"*", "-", "*", "/", "==", "!=", "&&"};
  return gen_rand_from_list(binary_ops, ARRLEN(binary_ops));
}


static char *gen_rand_unary_op() {
  static char *unary_ops[] = {/*"*",*/ "-"};
  return gen_rand_from_list(unary_ops, ARRLEN(unary_ops));
}

static char *gen_rand_expr_worker(int level) {
  char *s_l;
  char *s_r;
  char *str;

  int decision = level > 10 ? 0 : (rand() % 4 + 1);
  int space_l, space_r;
  char *s_expr;
  char *s_op;

  int len;
  switch (decision) {
  case 0:
    return gen_rand_decimal_uint32();
    break;

  case 1:
    s_l = gen_rand_expr_worker(level + 1);
    s_op = gen_rand_binary_op();
    s_r = gen_rand_expr_worker(level + 1);

    len = strlen(s_l) + strlen(s_r) + strlen(s_op) + 1;
    str = malloc(len);
    snprintf(str, len, "%s%s%s", s_l, s_op, s_r);
    free(s_l);
    free(s_r);
    free(s_op);
    return str;
    break;

  case 2:
    s_expr = gen_rand_expr_worker(level + 1);
    len = strlen(s_expr) + 3;
    str = malloc(len);
    snprintf(str, len, "(%s)", s_expr);
    free(s_expr);
    return str;
    break;

  case 3:
    s_expr = gen_rand_expr_worker(level + 1);
    s_op = gen_rand_unary_op();
    len = strlen(s_expr) + strlen(s_op) + 1;
    str = malloc(len);
    snprintf(str, len, "%s%s", s_op, s_expr);
    free(s_expr);
    free(s_op);
    return str;
    break;

  default:
    s_expr = gen_rand_expr_worker(level + 1);
    space_l = rand() % 5 + 1;
    space_r = rand() % 5 + 1;
    int len = strlen(s_expr) + space_l + space_r + 1;
    str = malloc(len);
    snprintf(str, len, "%*c%s%*c", space_l, ' ', s_expr, space_r, ' ');
    free(s_expr);
    return str;
    break;
  }
}

static void gen_rand_expr() {
  char *s = gen_rand_expr_worker(0);
  int len = strlen(s);
  memcpy(buf, s, len);
  buf[len] = '\0';
  free(s);
}

int main(int argc, char *argv[]) {
  int seed = time(0);
  srand(seed);
  int loop = 1;
  if (argc > 1) {
    sscanf(argv[1], "%d", &loop);
  }
  int i;
  for (i = 0; i < loop; i ++) {
    gen_rand_expr();

    sprintf(code_buf, code_format, buf);

    FILE *fp = fopen("/tmp/.code.c", "w");
    assert(fp != NULL);
    fputs(code_buf, fp);
    fclose(fp);

    int ret = system("gcc /tmp/.code.c -Wdiv-by-zero -Werror -o /tmp/.expr 2> /dev/null");
    if (ret != 0) continue;

    fp = popen("/tmp/.expr", "r");
    assert(fp != NULL);

    uint32_t result;
    ret = fscanf(fp, "%u", &result);
    pclose(fp);

    printf("%u %s\n", result, buf);
  }
  return 0;
}
