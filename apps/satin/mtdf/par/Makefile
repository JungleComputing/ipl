APP_OPTIONS = 22

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
