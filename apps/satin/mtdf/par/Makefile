APP_OPTIONS = 15

OUT= awari

include ../../../../config.mk
include ../../config.mk

java:
	$(JAVAC_COMMAND) Mtdf.java

satin: 
	$(IBISC_COMMAND) Mtdf.java

clean:
	rm -f $(TRASH_FILES)

include ../../run.mk
