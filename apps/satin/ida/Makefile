APP_OPTIONS = -f inputs/1 -max 72

MAIN = Ida.java
OUT= ida

include ../../../config.mk
include ../config.mk

satin:
	$(IBISC_COMMAND) -no-aborts $(MAIN)

clean:
	rm -f $(TRASH_FILES)

include ../run.mk
