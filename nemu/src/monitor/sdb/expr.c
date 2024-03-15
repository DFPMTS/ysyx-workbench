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

/* We use the POSIX regex functions to process regular expressions.
 * Type 'man regex' for more information about POSIX regex functions.
 */
#include <regex.h>

enum {
  TK_NOTYPE = 256, TK_EQ, TK_UNARY_MINUS, TK_UNSIGNED_NUM, 
  /* TODO: Add more token types */

};

static struct rule {
  const char *regex;
  int token_type;
} rules[] = {

  /* TODO: Add more rules.
   * Pay attention to the precedence level of different rules.
   */

  {"\\s+", TK_NOTYPE},    // spaces
  {"[0-9]+u", TK_UNSIGNED_NUM},   // decimal number
  {"\\+", '+'},         // plus
  {"-", '-'},           // minus
  {"\\*", '*'},         // mutiply
  {"/", '/'},           // divide
  {"\\(", '('},         // left parenthesis
  {")", ')'},           // right parenthesis
  {"==", TK_EQ},        // equal
};

#define NR_REGEX ARRLEN(rules)

static regex_t re[NR_REGEX] = {};

/* Rules are used for many times.
 * Therefore we compile them only once before any usage.
 */
void init_regex() {
  int i;
  char error_msg[128];
  int ret;

  for (i = 0; i < NR_REGEX; i ++) {
    ret = regcomp(&re[i], rules[i].regex, REG_EXTENDED);
    if (ret != 0) {
      regerror(ret, &re[i], error_msg, 128);
      panic("regex compilation failed: %s\n%s", error_msg, rules[i].regex);
    }
  }
}

typedef struct token {
  int type;
  char str[32];
} Token;

static Token tokens[4096] __attribute__((used)) = {};
static int nr_token __attribute__((used))  = 0;

static bool make_token(char *e) {
  int position = 0;
  int i;
  regmatch_t pmatch;

  nr_token = 0;

  while (e[position] != '\0') {
    /* Try all rules one by one. */
    for (i = 0; i < NR_REGEX; i ++) {
      if (regexec(&re[i], e + position, 1, &pmatch, 0) == 0 && pmatch.rm_so == 0) {
        char *substr_start = e + position;
        int substr_len = pmatch.rm_eo;

        Log("match rules[%d] = \"%s\" at position %d with len %d: %.*s",
            i, rules[i].regex, position, substr_len, substr_len, substr_start);

        position += substr_len;

        /* TODO: Now a new token is recognized with rules[i]. Add codes
         * to record the token in the array `tokens'. For certain types
         * of tokens, some extra actions should be performed.
         */
        Token* cur_token;
        switch (rules[i].token_type) {
        case TK_NOTYPE:
          // skip empty token
          break;
        default:
          if (nr_token >= ARRLEN(tokens)) {
            panic("Too many tokens in expression!");
          }                    
          cur_token = &tokens[nr_token];
          if(substr_len >= ARRLEN(cur_token->str)){
            panic("Token too long!");
          }
          memcpy(cur_token->str, substr_start, substr_len);
          cur_token->str[substr_len] = '\0';
          cur_token->type = rules[i].token_type;
          ++nr_token;
          break;
        }

        break;
      }
    }

    if (i == NR_REGEX) {
      printf("no match at position %d\n%s\n%*.s^\n", position, e, position, "");
      return false;
    }
  }

  return true;
}

// flag for expr evaluation
static bool eval_success;

static word_t eval_single_token(int i) {
  char *endptr;
  Token *token = &tokens[i];
  if (token->type == TK_UNSIGNED_NUM) {
    // first put in UL
    unsigned long val_ul = strtoul(tokens[i].str, &endptr, 10);
    if (endptr == tokens[i].str) {
      // invalid number
      Log("Invalid unsigned decimal number."); 
      eval_success = false;
    }

#ifndef CONFIG_ISA64
    // check for UL to word_t overflow
    if (val_ul > UINT32_MAX) {
      // overflow
      Log("Decimal number overflow.");
      eval_success = false;
    }
#endif
    return (word_t)val_ul;
  } else {
    Log("Invalid single token type: %d", token->type);
    eval_success = false;
    // invalid single token
    return -1;
  }
}

static bool is_paren_match(int l, int r) {  
  int left_paren_count = 0;

  for (int i = l; i <= r; ++i) {
    if (tokens[i].type == '(') {
      ++left_paren_count;
    } else if (tokens[i].type == ')') {
      if (!left_paren_count) {
        return false;
      } else {
        --left_paren_count;
      }
    }
  }
  if (left_paren_count)
    return false;

  return true;
}

static bool op_in_list(int type, int *op_list, int op_list_len) {
  for (int i = 0; i < op_list_len; ++i) {
    if (type == op_list[i])
      return true;
  }
  return false;
}

static int find_leftmost_split(int l, int r, int *op_list, int op_list_len) {
  for (int i = r; i >= l; --i) {
    if (op_in_list(tokens[i].type, op_list, op_list_len) &&
        is_paren_match(l, i - 1) && is_paren_match(i + 1, r)) {
      // can split here
      return i;
    }
  }
  return -1;
}

static int get_op_with_lowest_precedence(int l, int r) {
  // op precedence list
  static int cmp_op_list[] = {TK_EQ};
  static int add_sub_op_list[] = {'+', '-'};
  static int mul_div_op_list[] = {'*', '/'};

  // split pos
  int pos = -1;

  pos = find_leftmost_split(l, r, cmp_op_list, ARRLEN(cmp_op_list));
  if (pos != -1)
    return pos;

  pos = find_leftmost_split(l, r, add_sub_op_list, ARRLEN(add_sub_op_list));
  if (pos != -1)
    return pos;

  pos = find_leftmost_split(l, r, mul_div_op_list, ARRLEN(mul_div_op_list));
  if (pos != -1)
    return pos;
  
  // failed, unable to find split pos
  return -1;
}

static word_t eval_expr(int l, int r) {
  printf("Now evaluating %d, %d\n",l,r);
  // the binary operator with lowest precedence
  int pos = -1;
  if (l > r) {
    // bad expression
    eval_success = false;
    return -1;
  } else if (l == r) {
    // single token
    word_t val = eval_single_token(l);
    if(!eval_success)
      return -1;
    return val;
  } else if(tokens[l].type == '(' && tokens[r].type == ')' && is_paren_match(l + 1, r - 1)){
    // remove paren
    return eval_expr(l + 1, r - 1);
  } else if((pos = get_op_with_lowest_precedence(l, r)) != -1){    
    // "main" operator
    int op_type = tokens[pos].type;

    // eval left part and right part
    word_t left_val = eval_expr(l, pos - 1);
    word_t right_val = eval_expr(pos + 1, r);
    word_t expr_val = -1;
    if(!eval_success)
      return -1;

    switch (op_type) {
    case '+':
      expr_val = left_val + right_val;
      break;
    case '-':
      expr_val = left_val - right_val;
      break;
    case '*':
      expr_val = left_val * right_val;
      break;
    case '/':
      // ATTENTION: div by zero
      if (right_val == 0) {
        eval_success = false;
        Log("Div by zero.");
      } else {
        expr_val = left_val / right_val;
      }
      break;
    case TK_EQ:
      expr_val = (left_val == right_val);
      break;

    default:
      Log("Invalid binary operator");
      eval_success = false;
      break;
    }

    return expr_val;
  } else {
    // unary operator
    if (tokens[l].type == TK_UNARY_MINUS) {
      return -eval_expr(l + 1, r);
    } else {
      eval_success = false;
      Log("Invalid unary minus");
      return -1;
    }
  }
}

// minus -> unary minus
static void fix_op_types() {
  static int before_expr_op_list[] = {'+', '-', '*', '/', TK_EQ, '('};
  for (int i = nr_token - 1; i >= 0; --i) {
    if (tokens[i].type == '-' &&
        (i == 0 || op_in_list(tokens[i - 1].type, before_expr_op_list,
                              ARRLEN(before_expr_op_list)))) {
      tokens[i].type = TK_UNARY_MINUS;
    }
  }
}

word_t expr(char *e, bool *success) {
  if (!make_token(e)) {
    *success = false;
    return 0;
  }

  /* TODO: Insert codes to evaluate the expression. */
  // TODO();
  for (int i = 0; i < nr_token; ++i) {
    printf("%10d", i);
  }
  puts("");

  for (int i = 0; i < nr_token; ++i) {
    printf("%10s", tokens[i].str);
  }
  puts("");

  fix_op_types();

  eval_success = true;  
  word_t val = eval_expr(0, nr_token - 1);
  *success = eval_success;
  
  return val;
}
