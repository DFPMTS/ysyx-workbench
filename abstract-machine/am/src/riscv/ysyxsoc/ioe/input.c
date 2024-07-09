#include "ysyxsoc.h"
#include "at_scancode.h"
#include <am.h>


#define MAP0(k) [GET_FIRST(AT_SCANCODE_##k)] = AM_KEY_##k,
#define MAP1(k) [GET_SECOND_OR_DEFAULT(AT_SCANCODE_##k,0xE0)] = AM_KEY_##k,

static int keymap0[256] = {
  AM_KEYS(MAP0)
};

static int keymap1[256] = {
  AM_KEYS(MAP1)
};

static bool escape = false;
static bool keydown = true;

static void reset_readkey_state()
{
  escape = false;
  keydown = true;
}

static void interpret_key(AM_INPUT_KEYBRD_T *kbd, int key)
{
  kbd->keydown = keydown;
  kbd->keycode = escape ? keymap1[key] : keymap0[key];
}

int printf(char*,...);

void __am_input_keybrd(AM_INPUT_KEYBRD_T *kbd) {
  kbd->keydown = true;
  kbd->keycode = AM_KEY_NONE;

  /*
  normal KEYDOWN:  [KEY]
  normal KEYUP:    0xF0 [KEY]
  escape KEYDOWN:  0xE0 [KEY]
  escape KEYUP:    0xE0 0xF0 [KEY]
  */

  int key;
  while ((key = inb(INPUT_BASE)) != 0) {
    if (key == 0xF0) {
      // break
      keydown = false;
    } else if (key == 0xE0) {
      // escape
      escape = true;
    } else {
      interpret_key(kbd, key);
      reset_readkey_state();
      return;
    }
  }
}
