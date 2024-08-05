#include <NDL.h>
#include <SDL.h>
#include <assert.h>
#include <string.h>

#define keyname(k) #k,
#define ARRLEN(arr) (sizeof(arr) / sizeof(arr[0]))

static const char *keyname[] = {
  "NONE",
  _KEYS(keyname)
};

static int find_key(const char *s){
  for (int i = 0; i < ARRLEN(keyname); ++i) {
    if(strcmp(keyname[i], s) == 0){
      return i;
    }
  }
  return 0;
}

int SDL_PushEvent(SDL_Event *ev) {
  return 0;
}

int SDL_PollEvent(SDL_Event *ev) {
  return 0;
}

int SDL_WaitEvent(SDL_Event *event) {
  if(!event){
    return 1;
  }
  char buf[64];
  char op[10];
  char key[20];
  while(!NDL_PollEvent(buf,sizeof(buf)));
  sscanf(buf, "%s %s", op, key);
  if (strcmp(op, "kd") == 0) {
    event->type = SDL_KEYDOWN;
  } else if (strcmp(op, "ku") == 0) {
    event->type = SDL_KEYUP;
  } else {
    assert(0);
  }
  event->key.keysym.sym = find_key(key);
  return 1;
}

int SDL_PeepEvents(SDL_Event *ev, int numevents, int action, uint32_t mask) {
  return 0;
}

uint8_t* SDL_GetKeyState(int *numkeys) {
  return NULL;
}
