APP_OPTIONS = 36

SRC  = NoverKInterface.java

MAIN = NoverK.java
OUT= noverk

SERIALIZABLE	=
SERIALIZABLE	+= NoverK.java

include ../../../config.mk
include ../config.mk

java: $(CLASS_FILES) $(MAIN_CLASS)

satin: java
	$(SATINC_COMMAND) -no-aborts $(MAIN_CLASS_NAME)

clean:
	rm -f $(TRASH_FILES)

include ../run.mk
