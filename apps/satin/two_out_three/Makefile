APP_OPTIONS = 44

MAIN = TwoOutThree.java
OUT= two_out_three

include ../../../config.mk
include ../config.mk

java: 
	$(JAVAC_COMMAND) $(MAIN)

satin:
	$(IBISC_COMMAND) $(MAIN)

clean:
	rm -f $(TRASH_FILES)

include ../run.mk
