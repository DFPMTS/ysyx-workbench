#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include <stdarg.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

typedef struct FormatOptions{
  char *out;
  const char *fmt;
  
  // flags
  int is_sharp; //  #  The value should be converted to an "alternate form".
  int is_zero;  //  0  The value should be zero padded
  int is_right; //  -  The converted value is to be left adjusted on the field boundary. (Overrides 0)
  int is_blank; // ' ' A  blank should be left before a positive number (or empty string) produced by a signed conversion
  int is_sign;  //  +  A sign (+ or -) should always be placed before a number produced by a signed  conversion.

  char padding;

  // field width
  int width;    // An optional decimal digit string (with nonzero first digit) specifying a minimum field width.
  
  // precision
  // TODO
  
  // length modifier
  // TODO

  // conversion specifiers are handled in xprintf main body

  // additional options
  int base;
  int upper_case;
  int neg;
  int pointer;
}FormatOptions;

static int isdigit(char c)
{
  return  '0' <= c && c <= '9';
}

static const char *str_to_int(const char *s, int *x) {
  int res = 0;
  int i;
  for (i = 0; isdigit(s[i]); ++i) {
    res = (res * 10) + (s[i] - '0');
  }
  *x = res;
  return &s[i];
}

static void reset_opts(FormatOptions *opt)
{
  opt->is_sharp = 0;
  opt->is_zero = 0;
  opt->is_right = 0;
  opt->is_blank = 0;
  opt->is_sign = 0;
  opt->width = 0;
  opt->base = 10;
  opt->padding = ' ';  
  opt->neg = 0;
  opt->pointer = 0;
}

// %[$][flags][width][.precision][length modifier]conversion

static void collect_flags(FormatOptions *opt)
{
  while(*opt->fmt != '\0'){
    switch (*opt->fmt)
    {
    case '#':
      panic("Flag # not implemented");
      break;
    case '0':      
      opt->is_zero = 1;
      break;
    case '-':
      panic("Flag - not implemented");
      break;
    case ' ':
      panic("Flag (blank) not implemented");
      break;
    case '+':
      panic("Flag + not implemented");
      break;  
    default:
      // end of flags, configure settings
      // ! Since we only support '0' in flags, for now we omit the override
      if (opt->is_zero) {
        /*  For d, i, o, u, x, X, a, A, e, E, f, F, g, and G conversions, the converted value is padded on the left with zeros rather than blanks.
            If the 0 and - flags both appear, the 0 flag is ignored. 
            If a precision is given with a numeric conversion (d, i, o, u, x, and X), the 0 flag is ignored. 
          ! For other conversions, the behavior is undefined.
        */
        opt->padding = '0';
      }
      return;
    }
    opt->fmt++;
  }  
}

static void collect_width(FormatOptions *opt)
{
  if(isdigit(*opt->fmt) && *opt->fmt != '0'){
    opt->fmt = str_to_int(opt->fmt, &opt->width);
  }  
}

// don't do padding
static void out_char(FormatOptions *opt, char c)
{
  if(opt->out == NULL){
    putch(c);
  }else{
    *opt->out++ = c;
  }
}

// pad real_len to opt->width
static void pad(FormatOptions *opt, int real_len)
{
  for (int i = 0; i < opt->width - real_len; ++i) {
    out_char(opt, opt->padding);
  }
}

static void out_str(FormatOptions *opt, char *s)
{
  pad(opt, strlen(s));
  while (*s != '\0') {
    out_char(opt, *s++);
  }
}

static void out_int(FormatOptions *opt, unsigned int x) {
  static char decimal[10] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
  static char hex_lower[16] = {'0', '1', '2', '3', '4', '5', '6', '7',
                               '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
  static char hex_upper[16] = {'0', '1', '2', '3', '4', '5', '6', '7',
                               '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

  // choose the right digit-to-char map
  char *map;
  if(opt->base == 10){
    map = decimal;
  }else if(opt->base == 16){
    if(opt->upper_case){
      map = hex_upper;
    }else{
      map = hex_lower;
    }
  }else{
    panic("Invalid base");
  }
  
  // integer -> string
  size_t size = 0;
  char s[30];
  if (x == 0) {
    s[0] = map[0];
    size = 1;
  } else {
    while (x) {
      s[size++] = map[x % opt->base];
      x /= opt->base;
    }
  }

  // pointer needs a "0x" prefix
  if(opt->pointer){
    out_char(opt,'0');
    out_char(opt,'x');
  }

  // * special case: if padding='0' and <0, the output should be: -00000010
  if (opt->neg && opt->padding == '0') {
    out_char(opt, '-');
    // note that we have already output the '-'
    pad(opt, size + 1);
  } else {    
    // negative sign
    if (opt->neg) {
      s[size++] = '-';
    }
    pad(opt, size);
  }

  // reverse
  for (int i = size - 1; i >= 0; --i) {
    out_char(opt,s[i]);
  }
}


static int xprintf(char *out, const char *fmt, va_list ap) {
  int printed = 0;
  FormatOptions opt;
  opt.fmt = fmt;
  opt.out = out;
  int x;
  while (*opt.fmt != '\0') {
    char c = *opt.fmt;
    if (c == '%') {
      ++opt.fmt;      
      reset_opts(&opt);
      // %[$][flags][width][.precision][length modifier]conversion
      collect_flags(&opt);
      collect_width(&opt);      
      // TODO precision
      // TODO length modifier
      // conversion
      c = *opt.fmt;
      switch (c){
        case 'd':
        case 'i':
          ++printed;
          opt.base = 10;
          x = va_arg(ap, int);
          opt.neg = x < 0 ? 1 : 0; // neg sign
          out_int(&opt, (unsigned int)abs(x));
          break;

        case 'u':
        case 'x':
        case 'X':
          ++printed;
          opt.neg = 0;
          opt.base = (c == 'u') ? 10 : 16;
          opt.upper_case = (c == 'X') ? 1 : 0;
          out_int(&opt, va_arg(ap, unsigned int));
          break;

        case 'p':
          // this only works on 32-bit platforms
          ++printed;
          opt.neg = 0;
          opt.base =  16;
          opt.upper_case = 1;
          opt.padding = '0';
          opt.width = 8;
          opt.pointer = 1;
          out_int(&opt, va_arg(ap, unsigned int));
          break;

        case 's':
          ++printed;
          out_str(&opt, va_arg(ap, char *));
        break;

        case 'c':
          ++printed;
          // ! we have to pad here since out_char don't do padding
          pad(&opt, 1);
          out_char(&opt, (char)va_arg(ap, int));
          break;

        case '%':
          out_char(&opt,'%');
          break;

        default: 
          panic("Invalid format string");
      }      
    } else {
      out_char(&opt, *opt.fmt);
    }
    opt.fmt++;
  }
  if (opt.out != NULL)
    *opt.out = '\0';
  return printed;
}

int printf(const char *fmt, ...) {
  va_list ap;

  va_start(ap,fmt);
  int ret_val = xprintf(NULL, fmt, ap);
  va_end(ap);

  return ret_val;
}

int vsprintf(char *out, const char *fmt, va_list ap) {  
  return xprintf(out, fmt, ap);
}

int sprintf(char *out, const char *fmt, ...) {
  va_list ap;

  va_start(ap,fmt);
  int ret_val = xprintf(out, fmt, ap);
  va_end(ap);

  return ret_val;
}

int snprintf(char *out, size_t n, const char *fmt, ...) {
  panic("Not implemented");
}

int vsnprintf(char *out, size_t n, const char *fmt, va_list ap) {
  panic("Not implemented");
}

#endif
