APP_OPTIONS = 9678904321

SRC  = PrimFacInterface.java

MAIN = PrimFac.java
OUT= primfac

SERIALIZABLE	=
SERIALIZABLE	+= PrimFac.java

include ../../../config.mk
include ../config.mk

java: $(CLASS_FILES) $(MAIN_CLASS)

satin: java
	$(SATINC_COMMAND) -no-aborts $(MAIN_CLASS_NAME)

manta: $(MAIN) $(O_FILES)
	$(MANTAC_COMMAND) -no-aborts -o $(OUT) $(MAIN) $(O_FILES) ../../../../ibis/$(IBIS_LIB)

clean:
	rm -f $(TRASH_FILES)

include ../run.mk
