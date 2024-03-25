#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include <stdarg.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

static char* sprintf_int(char *out, int x) {
  char a[20];
  size_t i = 0;

  while (x) {
    a[i++] = (x % 10) + '0';
    x /= 10;
  }
  if (!i) {
    a[i++] = '0';
  }
  while (i) {
    *out++ = a[--i];
  }
  return out;
}

static char *sprintf_str(char *out, char *s) {
  while (*s != '\0') {
    *out++ = *s++;
  }
  return out;
}

// %[$][flags][width][.precision][length modifier]conversion
int printf(const char *fmt, ...) {
  panic("Not implemented");
}

int vsprintf(char *out, const char *fmt, va_list ap) {
  int printed = 0;
  while (*fmt != '\0') {
    if (*fmt == '%') {    
      char c = *(++fmt);
      if (c == 'd') {
        ++printed;
        out = sprintf_int(out, va_arg(ap, int));
      } else if (c == 's') {
        ++printed;
        out = sprintf_str(out, va_arg(ap, char *));
      } else if (c == '%') {
        *out++ = '%';
      }
    } else {
      *out++ = *fmt;
    }
    fmt++;
  }
  *out = '\0';
  return printed;
}

int sprintf(char *out, const char *fmt, ...) {
  va_list args;
  va_start(args,fmt);
  int ret_val = vsprintf(out, fmt, args);
  va_end(args);

  return ret_val;
}

int snprintf(char *out, size_t n, const char *fmt, ...) {
  panic("Not implemented");
}

int vsnprintf(char *out, size_t n, const char *fmt, va_list ap) {
  panic("Not implemented");
}

#endif
