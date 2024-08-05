#include <unistd.h>
#include <stdio.h>
#include <fixedptc.h>
#include <assert.h>

#define R(x) fixedpt_rconst(x)
#define A 1.25
#define B -2.5
#define C -1000.625
#define D 3131.125

int main() {
  fixedpt a = R(A), b = R(B), c = R(C), d = R(D);

  // mul
  assert(fixedpt_mul(a, b) == R(A * B));

  // muli
  assert(fixedpt_muli(a, 144) == R(A * 144));

  // div
  assert(fixedpt_div(d, b) == R(D / B));
  printf("%f %f\n", fixedpt_tofloat(fixedpt_div(d, b)), D / B);

  // divi
  assert(fixedpt_divi(d, 144) == R(D / 144));
  printf("%f %f\n", fixedpt_tofloat(fixedpt_divi(d, 144)), D / 144);

  // ceil 
  assert(fixedpt_ceil(a) == fixedpt_rconst(2));
  assert(fixedpt_ceil(b) == fixedpt_rconst(-2));
  

  // floor
  assert(fixedpt_floor(c) == fixedpt_rconst(-1001));
  assert(fixedpt_floor(d) == fixedpt_rconst(3131));

  return 0; 
}
