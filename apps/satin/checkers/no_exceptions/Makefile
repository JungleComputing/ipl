APP_OPTIONS =

SRC  = Position.java
SRC += OrcaRandom.java
SRC += Result.java

MAIN = Checkers.java
OUT= checkers

SERIALIZABLE	=
SERIALIZABLE	+= Checkers.java

include ../../../../config.mk
include ../../config.mk

java: $(CLASS_FILES) $(MAIN_CLASS)

satin: java
	$(SATINC_COMMAND) $(MAIN_CLASS_NAME)

clean:
	rm -f $(TRASH_FILES)

include ../../run.mk
