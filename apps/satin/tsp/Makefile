APP_OPTIONS = 19 -bound 340

SRC  = DistanceTable.java
SRC += OrcaRandom.java
SRC += TspInterface.java

MAIN = Tsp.java
OUT= tsp

SERIALIZABLE	=
SERIALIZABLE	+= DistanceTable.java
SERIALIZABLE	+= Tsp.java

include ../../../config.mk
include ../config.mk

java: $(CLASS_FILES) $(MAIN_CLASS)

satin: java
	$(SATINC_COMMAND) -no-aborts $(MAIN_CLASS_NAME)
	$(IOC_COMMAND) Satin_*.class

manta: $(MAIN) $(O_FILES)
	$(MANTAC_COMMAND) -o $(OUT) $(MAIN) $(O_FILES) ../../../../ibis/$(IBIS_LIB)

clean:
	rm -f $(TRASH_FILES)

include ../run.mk
