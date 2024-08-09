#include <klib.h>
#include <klib-macros.h>
#include <stdint.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

size_t strlen(const char *s) {
  size_t len = 0;
  while (s[len] != '\0') {
    ++len;
  }
  return len;
}

char *strcpy(char *dst, const char *src) {
  strncpy(dst, src, strlen(src) + 1);
  return dst;
}

char *strncpy(char *dst, const char *src, size_t n) {
  size_t i;
  for (i = 0; i < n && src[i] != '\0'; ++i) {
    dst[i] = src[i];
  }
  for (; i < n; ++i) {
    dst[i] = '\0';
  }
  return dst;
}

char *strcat(char *dst, const char *src) {
  strncpy(dst + strlen(dst), src, strlen(src) + 1);
  return dst;
}

// ...The comparison is done using unsigned characters.
int strcmp(const char *s1, const char *s2) {
  return strncmp(s1, s2, -1);
}

int strncmp(const char *s1, const char *s2, size_t n) {
  size_t i;
  const unsigned char *us1 = (const unsigned char *)s1, *us2 = (const unsigned char *)s2;
  for (i = 0; us1[i] == us2[i] && i < n; ++i) {
    if (us1[i] == '\0') {
      return 0;
    }
  }
  return (i == n) ? 0 : us1[i] - us2[i];
}

void *memset(void *s, int c, size_t n) {
  size_t i;
  unsigned char *us = s;
  for (i = 0; i < n; ++i) {
    us[i] = (unsigned char)c;
  }
  return s;
}

void *memmove(void *dst, const void *src, size_t n) {
  panic("Not implemented");
}

void *memcpy(void *out, const void *in, size_t n) {
  unsigned char *uout = out;
  const unsigned char *uin = in;
  size_t i;
  for (i = 0; i < n; ++i) {
    uout[i] = uin[i];
  }
  return out;
}

int memcmp(const void *s1, const void *s2, size_t n) {
  const unsigned char *us1 = s1, *us2 = s2;
  size_t i;
  for (i = 0; us1[i] == us2[i] && i < n - 1; ++i)
    ;
  return us1[i] - us2[i];
}

#endif
