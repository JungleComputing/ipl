SRC  = DasInfo.java 
SRC += Data.java
SRC += List.java
SRC += DList.java
SRC += Tree.java

MAIN = Main.java
OUT=main

include ../../../config.mk

APP_OPTIONS =
APP_OPTIONS += -verbose
APP_OPTIONS += -array
APP_OPTIONS += -arraysize 102400
APP_OPTIONS += -count 1000
APP_OPTIONS += -panda

include ../config.mk

java: $(CLASS_FILES) $(MAIN_CLASS)
	java ibis.frontend.io.IOGenerator Data.class
	java ibis.frontend.io.IOGenerator List.class
	java ibis.frontend.io.IOGenerator DList.class
	java ibis.frontend.io.IOGenerator Tree.class

manta: $(MAIN) $(O_FILES)
#	$(MANTAC_COMMAND) -o $(OUT) $(MAIN) $(O_FILES) ../../../../ibis/$(IBIS_LIB)

clean:
	rm -f $(TRASH_FILES)

include ../run.mk
