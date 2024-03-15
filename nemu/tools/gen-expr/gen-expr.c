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

static void gen_rand_uint32(char** s, int* len)
{
  uint32_t num = (uint32_t)((uint64_t)rand() * (uint64_t)rand()) % UINT32_MAX;

  int num_len = 0;
  if (num == 0) {
    char *num_str = malloc(5);
    num_str[0] = '0';
    num_str[1] = 'x';
    num_str[2] = '0';
    num_str[3] = 'u';
    num_str[4] = '\0';
    *s = num_str;
    *len = 4;
    return;
  }

  uint32_t t = num;
  char buf[20];
  while (t) {
    buf[num_len++] = (t % 10) + '0';
    t /= 10;
  }
  char *num_str = malloc(num_len + 4);
  num_str[0] = '0';
  num_str[1] = 'x';
  for (int i = 0; i < num_len; ++i) {
    num_str[i + 2] = buf[num_len - i - 1];
  }
  num_str[num_len + 2] = 'u';
  num_str[num_len + 3] = '\0';
  *s = num_str;
  *len = num_len + 3;
}

static void gen_rand_op(char **s, int*len)
{
  static char *ops[] = {"*", "-", "*", "/", "=="};
  int id = rand() % ARRLEN(ops);
  char *str;
  int str_len = strlen(ops[id]);
  str = malloc(str_len + 1);
  memcpy(str, ops[id], str_len + 1);
  *s = str;
  *len = str_len;
}

static void gen_rand_expr_worker(char** s, int* len, int level)
{
    char* s_l;
    int len_l;

    char* s_r;
    int len_r;

    char* str;

    int decision = level > 10 ? 0 : (rand() % 3 + 1);

    char *space_s;
    int space_l, space_r;

    char *s_expr;
    int len_expr;

    char *s_op;
    int len_op;
    switch (decision) {
    case 0:
        gen_rand_uint32(&s_l, &len_l);
        *s = s_l;
        *len = len_l;
        break;

    case 1:
        gen_rand_expr_worker(&s_l, &len_l, level + 1);
        gen_rand_expr_worker(&s_r, &len_r, level + 1);
        gen_rand_op(&s_op, &len_op);
        str = malloc(len_l + len_r + len_op + 1);
        
        memcpy(str, s_l, len_l);

        memcpy(str + len_l, s_op, len_op);

        memcpy(str + len_l + len_op, s_r, len_r);
        str[len_l + len_r + len_op] = '\0';

        *s = str;
        *len = len_l + len_r + len_op;
        free(s_l);
        free(s_r);
        free(s_op);
        break;

    case 2:
        gen_rand_expr_worker(&s_l, &len_l, level + 1);
        *s = malloc(len_l + 3);
        str = *s;
        memcpy(str + 1, s_l, len_l);
        str[0] = '(';
        str[len_l + 1] = ')';
        str[len_l + 2] = '\0';
        *len = len_l + 2;
        free(s_l);
        break;

    default:
      gen_rand_expr_worker(&s_expr, &len_expr, level + 1);
      space_l = rand() % 5 + 1;
      space_r = rand() % 5 + 1;
      space_s = malloc(len_expr + space_l + space_r + 1);
      for (int i = 0; i < space_l; ++i) {
        space_s[i] = ' ';
      }

      for (int i = space_l; i < space_l + len_expr; ++i) {
        space_s[i] = s_expr[i - space_l];
      }
      memcpy(space_s + space_l, s_expr, len_expr);

      for (int i = space_l + len_expr; i < space_l + len_expr + space_r; ++i) {
        space_s[i] = ' ';
      }
      space_s[space_l + len_expr + space_r] = '\0';
      free(s_expr);
      *s = space_s;
      *len = len_expr + space_l + space_r;
      break;
    }
    
}

static void gen_rand_expr()
{
    char* s;
    int len;
    gen_rand_expr_worker(&s, &len, 0);
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

    int result;
    ret = fscanf(fp, "%d", &result);
    pclose(fp);

    printf("%u %s\n", result, buf);
  }
  return 0;
}
