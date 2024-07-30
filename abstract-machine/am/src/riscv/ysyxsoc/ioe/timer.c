#include <am.h>
#include "ysyxsoc.h"

#define RTC_ADDR 0x02000000

static uint64_t boot_time;

static uint64_t read_rtc()
{
  return ((uint64_t)inl(RTC_ADDR + 4) << 32) + inl(RTC_ADDR);
}

void __am_timer_init() {
  boot_time = read_rtc();
}

void __am_timer_uptime(AM_TIMER_UPTIME_T *uptime) {
  uptime->us = read_rtc() - boot_time;
}

void __am_timer_rtc(AM_TIMER_RTC_T *rtc) {
  rtc->second = 0;
  rtc->minute = 0;
  rtc->hour   = 0;
  rtc->day    = 0;
  rtc->month  = 0;
  rtc->year   = 1900;
}
