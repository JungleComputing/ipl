SRC  = Test.java 
SRC += myServer.java 
SRC += DasInfo.java 

MAIN = Main.java
OUT=main

include ../../../config.mk

java: $(CLASS_FILES) $(MAIN_CLASS)
	rm -f rmi_s*_Test.java
	java ibis.frontend.rmi.Main -v Test
	$(JAVAC_COMMAND) rmi_s*.java

manta: $(MAIN) $(O_FILES)
	$(MANTAC_COMMAND) -o $(OUT) $(MAIN) $(O_FILES) ../../../../ibis/$(IBIS_LIB)

clean:
	rm -f $(TRASH_FILES) rmi_s*_Test.java







