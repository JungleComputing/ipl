APP_OPTIONS = 20 -bound 350

MAIN = Tsp.java
OUT= tsp

include ../../../config.mk
include ../config.mk

satin:
	$(IBISC_COMMAND) -no-aborts $(MAIN)

clean:
	rm -f $(TRASH_FILES)

include ../run.mk
