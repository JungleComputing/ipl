APP_OPTIONS =

SRC  = Position.java
SRC += OrcaRandom.java
SRC += Result.java

MAIN = Checkers.java
OUT= checkers

include ../../../../config.mk
include ../../config.mk

java: $(CLASS_FILES) $(MAIN_CLASS)

satin: java
	$(SATINC_COMMAND) $(MAIN_CLASS_NAME)

manta: $(MAIN) $(O_FILES)
	$(MANTAC_COMMAND) -o $(OUT) $(MAIN) $(O_FILES) ../../../../ibis/$(IBIS_LIB)

clean:
	rm -f $(TRASH_FILES)

include ../../run.mk
