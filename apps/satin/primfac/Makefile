APP_OPTIONS = 19678904321

MAIN = PrimFac.java
OUT= primfac

include ../../../config.mk
include ../config.mk

java:
	$(JAVAC_COMMAND) $(MAIN)

satin:
	$(IBISC_COMMAND) -no-aborts $(MAIN)

clean:
	rm -f $(TRASH_FILES)

include ../run.mk
