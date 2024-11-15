#include "config.hpp"
#include <string>

std::string getEventName(int eventId);
int getEventId(const std::string &eventName);

uint64_t getEventCount(int eventId);
uint64_t getEventCount(const std::string &eventName);

void clearAllEventCount();

bool isCommit();