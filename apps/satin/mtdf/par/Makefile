APP_OPTIONS = 15

SRC             = AwariBoard.java
SRC            += Input.java
SRC            += Done.java
SRC            += Mtdf.java
SRC            += MtdfInterface.java
SRC            += NodeType.java
SRC            += TranspositionTable.java
SRC            += TranspositionTableEntry.java

MAIN		=  Main.java

OUT= awari

SERIALIZABLE	=
SERIALIZABLE	+= Mtdf.java
SERIALIZABLE	+= NodeType.java

include ../../../../config.mk
include ../../config.mk

java: $(CLASS_FILES) $(MAIN_CLASS)

satin: java
	$(SATINC_COMMAND) Mtdf

manta: $(MAIN) $(O_FILES)
	$(MANTAC_COMMAND) -o $(OUT) $(MAIN) $(O_FILES) ../../../../ibis/$(IBIS_LIB)

satinser: java
	$(IOC_COMMAND) *.class
	make satin
	$(IOC_COMMAND) Satin_*.class

clean:
	rm -f $(TRASH_FILES) Satin_*

include ../../run.mk
