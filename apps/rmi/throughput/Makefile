SRC  = Test.java 
SRC += myServer.java 
SRC += DasInfo.java 
SRC += Data.java
SRC += List.java
SRC += DList.java
SRC += Tree.java

MAIN = Main.java
OUT=main

include ../../../config.mk

java: $(CLASS_FILES) $(MAIN_CLASS)
	rm -f rmi_s*_Test.java
	java ibis.frontend.rmi.Main -v Test
	$(JAVAC_COMMAND) rmi_s*.java
	java ibis.frontend.io.IOGenerator Data.class
	java ibis.frontend.io.IOGenerator List.class
	java ibis.frontend.io.IOGenerator DList.class
	java ibis.frontend.io.IOGenerator Tree.class

manta: $(MAIN) $(O_FILES)
#	$(MANTAC_COMMAND) -o $(OUT) $(MAIN) $(O_FILES) ../../../../ibis/$(IBIS_LIB)

clean:
	rm -f $(TRASH_FILES) rmi_s*_Test.java







