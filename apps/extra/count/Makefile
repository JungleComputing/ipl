SRC = Tree.java
SRC = CountingOutputStream.java

MAIN = Main.java
OUT=main

include ../../../config.mk

java: $(CLASS_FILES) $(MAIN_CLASS)
	java ibis.frontend.io.IOGenerator Tree.class

manta: $(MAIN) $(O_FILES)
#	$(MANTAC_COMMAND) -o $(OUT) $(MAIN) $(O_FILES) ../../../../ibis/$(IBIS_LIB)

clean:
	rm -f $(TRASH_FILES) rmi_s*_Test.java







