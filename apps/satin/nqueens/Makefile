APP_OPTIONS = 20

SRC  = NQueensInterface.java

MAIN = NQueens.java
OUT= nqueens

include ../../../config.mk
include ../config.mk

java: $(CLASS_FILES) $(MAIN_CLASS)
#	cd abort; make java
#	cd simple_abort; make java
#	cd exceptions; make java

satin: java
	$(SATINC_COMMAND) -no-aborts $(MAIN_CLASS_NAME)
#	cd abort; make satin
#	cd simple_abort; make satin
#	cd exceptions; make satin

manta: $(MAIN) $(O_FILES)
	$(MANTAC_COMMAND) -o $(OUT) $(MAIN) $(O_FILES) ../../../../ibis/$(IBIS_LIB)
#	cd abort; make manta
#	cd simple_abort; make manta
#	cd exceptions; make manta

mantapar: $(O_FILES) satin
	$(MANTAC_COMMAND) -c Satin_NQueens_spawn_nqueens_InvocationRecord.java
	$(MANTAC_COMMAND) -c Satin_NQueens_spawn_nqueens_ReturnRecord.java
	$(MANTAC_COMMAND) -S -o $(OUT) $(MAIN_CLASS) *.o $(MANTA_LIB)

clean:
	rm -f $(TRASH_FILES)
	cd abort; make clean
	cd simple_abort; make clean
	cd exceptions; make clean

include ../run.mk
