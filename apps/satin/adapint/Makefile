APP_OPTIONS = 0 400000 0.00001

SRC  = AdapIntInterface.java

MAIN = AdapInt.java
OUT= adapint

SERIALIZABLE	=
SERIALIZABLE	+= AdapInt.java

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
