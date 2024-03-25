#include "sput.h"
#include <stdio.h>
#define panic(X) (X)

#include "klib_string.h"
#include "klib_stdio.h"


static char *s[] = {"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaab",
                    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                    ", World!\n",
                    "Hello, World!\n",
                    "#####"};

static char str1[] = "Hello";
static char str[20];

static void test_strlen() {
  sput_fail_unless(strlen("11111111") == 8, "11111111 len is 8");
  sput_fail_unless(strlen("31312\n") == 6, "31312\\n len is 6");
}

static void test_strcmp() {
  sput_fail_unless(strcmp(s[0], s[2]) == 0, "s[0] == s[2]");
  sput_fail_unless(strcmp(s[0], s[1]) < 0, "s[0] < s[1]");
  sput_fail_unless(strcmp(s[0] + 1, s[1] + 1) < 0, "s[0]+1 < s[1]+1");
  sput_fail_unless(strcmp(s[0] + 2, s[1] + 2) < 0, "s[0]+2 < s[1]+2");
  sput_fail_unless(strcmp(s[0] + 3, s[1] + 3) < 0, "s[0]+3 < s[1]+3");
}

static void test_strcpy() {
  strcpy(str, str1);
  sput_fail_if(strcmp(str, str1) != 0, "str and str1 should be equal");
}

static void test_strcat() {
  strcat(str, s[3]);
  sput_fail_if(strcmp(str, s[4]) != 0, "str and s[4] should be equal");
}

static void test_memcmp() {
  sput_fail_if(memcmp("abc", "abc", 3) != 0, "should equal");
  sput_fail_unless(memcmp("abcd", "abce", 4) < 0, "should less");
  sput_fail_unless(memcmp("abcd", "abce", 3) == 0, "should equal");
}

static void test_memset() {
  memset(str, '#', 5);
  printf("%d\n",memcmp(str, s[5], 5));
  sput_fail_if(memcmp(str, s[5], 5) != 0, "str and s[5] should be equal");
}

void test_snprintf() {
  char buf[100];
  sprintf(buf, "%d + %d = %d", 1, 1, 2);
  sput_fail_unless(strcmp(buf, "1 + 1 = 2") == 0, "%d");

  sprintf(buf, "%s%s = %s%s", "Close", "theWorld", "Open", "theNext");
  sput_fail_unless(strcmp(buf, "ClosetheWorld = OpentheNext") == 0, "%s");

  sprintf(buf, "%d%s = %d%s", 1, "min", 60, "s");
  sput_fail_unless(strcmp(buf, "1min = 60s") == 0, "%s + %d");

  sprintf(buf, "%%%%%%%%");
  sput_fail_unless(strcmp(buf, "%%%%") == 0, "%%");

  sput_fail_unless(sprintf(buf, "%% %s %d", "hoho", 123) == 2,"sprintf ret val");
}


int main() {
  sput_start_testing();

  sput_enter_suite("strlen");
  sput_run_test(test_strlen);

  sput_enter_suite("strcmp");
  sput_run_test(test_strcmp);

  sput_enter_suite("strcpy");
  sput_run_test(test_strcpy);

  sput_enter_suite("strcat");
  sput_run_test(test_strcat);

  sput_enter_suite("memcmp");
  sput_run_test(test_memcmp);

  sput_enter_suite("memset");
  sput_run_test(test_memset);


  sput_enter_suite("sprintf");
  sput_run_test(test_snprintf);

  sput_finish_testing();
}