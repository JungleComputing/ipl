#for a matrix of 1536x1536
APP_OPTIONS = 5 0 48

#for a matrix of 1024x1024
#APP_OPTIONS = 4 0 64

SRC  = MmultInterface.java
SRC += Leaf.java
SRC += Matrix.java

MAIN = Mmult.java
OUT= mmult

include ../../../config.mk
include ../config.mk

java: $(CLASS_FILES) $(MAIN_CLASS)

satin: java
	$(SATINC_COMMAND) -no-aborts $(MAIN_CLASS_NAME)

manta: $(MAIN) $(O_FILES)
	$(MANTAC_COMMAND) -o $(OUT) $(MAIN) $(O_FILES) ../../../../ibis/$(IBIS_LIB)

clean:
	rm -f $(TRASH_FILES)

include ../run.mk
