#APP_OPTIONS = 19
APP_OPTIONS = 17

OUT= awari

MAIN=Mtdf.java

include ../../../../config.mk
include ../../config.mk

java:
	$(JAVAC_COMMAND) $(MAIN)

satin: 
	$(IBISC_COMMAND) $(MAIN)

clean:
	rm -f $(TRASH_FILES)

include ../../run.mk
