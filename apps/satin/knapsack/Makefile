APP_OPTIONS = 30

SRC  = KnapsackInterface.java
SRC += OrcaRandom.java
SRC += Return.java

MAIN = Knapsack.java
OUT= knapsack

SERIALIZABLE	=
SERIALIZABLE	+= Knapsack.java
SERIALIZABLE	+= Return.java

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
