SRC  = Test.java 
SRC += myRep.java 
SRC += DasInfo.java 

MAIN = Main.java
OUT=main

include ../../../config.mk

java: $(CLASS_FILES) $(MAIN_CLASS)
	rm -f repmi_s*_Test.java
	java ibis.frontend.repmi.Main -v Test
	$(JAVAC_COMMAND) repmi_s*.java

clean:
	rm -f $(TRASH_FILES) repmi_s*_Test.java







