APP_OPTIONS = 21
#APP_OPTIONS = 17

OUT= awari

MAIN=Mtdf.java

include ../../../../config.mk
include ../../config.mk

java:
	$(JAVAC_COMMAND) $(MAIN)
	/usr/local/ibm-java/IBMJava2-131/bin/rmic TranspositionTable

satin: 
	$(IBISC_COMMAND) $(MAIN)
	/usr/local/ibm-java/IBMJava2-131/bin/rmic TranspositionTable

clean:
	rm -f $(TRASH_FILES)

include ../../run.mk
