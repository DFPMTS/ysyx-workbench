#include <SDL.h>
#include <ctype.h>
#include <nterm.h>
#include <stdarg.h>
#include <unistd.h>

char handle_key(SDL_Event *ev);

static void sh_printf(const char *format, ...) {
  static char buf[256] = {};
  va_list ap;
  va_start(ap, format);
  int len = vsnprintf(buf, 256, format, ap);
  va_end(ap);
  term->write(buf, len);
}

static void sh_banner() {
  sh_printf("Built-in Shell in NTerm (NJU Terminal)\n\n");
}

static void sh_prompt() { sh_printf("sh> "); }

static char *trim_white_space(char *cmd) {
  while (isspace(*cmd))
    ++cmd;
  int end = strlen(cmd);
  while (end > 0 && isspace(cmd[end - 1]))
    cmd[--end] = '\0';
  return cmd;
}

static void sh_handle_cmd(const char *cmd) {
  // copy cmd since strtok_r will modify string
  char copy[256];
  strcpy(copy, cmd);

  // upto 16 args
  char *argv[16];
  int argc = 0;
  /*
  The standard white-space characters are the following:
  space (' '), form feed ('\f'), new-line ('\n'),
  carriage return ('\r'), horizontal tab ('\t'),
  and vertical tab ('\v').
  C11dr §7.4.1.10 2
  */

  // get command
  const char *standard_white_space = " \f\n\r\t\v";
  char *saved_ptr;
  char *command = strtok_r(copy, standard_white_space, &saved_ptr);
  // no command found
  if (!command) {
    return;
  }

  // first arg is filename
  argv[argc++] = command;

  // collect other args
  char *cur_arg;
  while (cur_arg = strtok_r(NULL, standard_white_space, &saved_ptr)) {
    argv[argc++] = cur_arg;
  }
  argv[argc] = NULL;
  execvp(command, argv);
}

void builtin_sh_run() {
  sh_banner();
  sh_prompt();
  setenv("PATH", "/bin:/usr/bin", 0);
  while (1) {
    SDL_Event ev;
    if (SDL_PollEvent(&ev)) {
      if (ev.type == SDL_KEYUP || ev.type == SDL_KEYDOWN) {
        const char *res = term->keypress(handle_key(&ev));
        if (res) {
          sh_handle_cmd(res);
          sh_prompt();
        }
      }
    }
    refresh_terminal();
  }
}
