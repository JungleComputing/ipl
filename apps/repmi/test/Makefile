SRC  = Test.java 
SRC += myRep.java 
SRC += DasInfo.java 

MAIN = Main.java
OUT=main

include ../../../config.mk

java: $(CLASS_FILES) $(MAIN_CLASS)
	rm -f repmi_s*_Test.java
	java manta.frontend.repmi.Main -v Test
	$(JAVAC_COMMAND) repmi_s*.java

manta: $(MAIN) $(O_FILES)
	$(MANTAC_COMMAND) -o $(OUT) $(MAIN) $(O_FILES) ../../../../ibis/$(IBIS_LIB)

clean:
	rm -f $(TRASH_FILES) repmi_s*_Test.java







