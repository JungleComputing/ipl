APP_OPTIONS = -f ../seq/awaripos/01 21
#APP_OPTIONS = 17

OUT= awari

MAIN=Mtdf.java

include ../../../../config.mk
include ../../config.mk

#java:
#	$(JAVAC_COMMAND) $(MAIN)

satin: 
	$(IBISC_COMMAND) -keep $(MAIN)

clean:
	rm -f $(TRASH_FILES)

include ../../run.mk
