APP_OPTIONS = 0 3200000 0.000001

MAIN = AdapInt.java
OUT= adapint

include ../../../config.mk
include ../config.mk

satin:
	$(IBISC_COMMAND) -no-aborts $(MAIN)

clean:
	rm -f $(TRASH_FILES)

include ../run.mk
