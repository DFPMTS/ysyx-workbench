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

static uint8_t keystate[ARRLEN(keyname)];

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

static void parse_event(SDL_Event *ev, char *buf)
{
  char op[10];
  char key[20];
  sscanf(buf, "%s %s", op, key);
  ev->key.keysym.sym = find_key(key);
  if (strcmp(op, "kd") == 0) {
    ev->type = SDL_KEYDOWN;
    keystate[ev->key.keysym.sym] = 1;
  } else if (strcmp(op, "ku") == 0) {
    ev->type = SDL_KEYUP;
    keystate[ev->key.keysym.sym] = 0;
  } else {
    assert(0);
  }  
}

// ! NOTE: if event == NULL, the event should NOT be remove from queue
int SDL_PollEvent(SDL_Event *ev) {
  if(!ev){
    return 1;
  }
  char buf[64]  ;
  if (NDL_PollEvent(buf,sizeof(buf))){
    parse_event(ev, buf);
    return 1;
  } else {
    return 0;
  }
}

// ! NOTE: if event == NULL, the event should NOT be remove from queue
int SDL_WaitEvent(SDL_Event *event) {
  if(!event){
    return 1;
  }
  char buf[64];
  while(!NDL_PollEvent(buf,sizeof(buf)));
  parse_event(event, buf);

  return 1;
}

int SDL_PeepEvents(SDL_Event *ev, int numevents, int action, uint32_t mask) {
  return 0;
}

uint8_t* SDL_GetKeyState(int *numkeys) {
  return keystate;
}
