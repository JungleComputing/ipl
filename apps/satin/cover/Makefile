APP_OPTIONS = 32

SRC  = CoverInterface.java
SRC += OrcaRandom.java
SRC += Return.java

MAIN = Cover.java
OUT= cover

SERIALIZABLE	=
SERIALIZABLE	+= Cover.java
SERIALIZABLE	+= Return.java

include ../../../config.mk
include ../config.mk

java: $(CLASS_FILES) $(MAIN_CLASS)

satin: java
	$(SATINC_COMMAND) -no-aborts $(MAIN_CLASS_NAME)

clean:
	rm -f $(TRASH_FILES)

include ../run.mk
