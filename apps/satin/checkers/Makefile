APP_OPTIONS =

SRC  = Position.java
SRC += OrcaRandom.java
SRC += Result.java

MAIN = Checkers.java
OUT= checkers

SERIALIZABLE	=
SERIALIZABLE	+= Checkers.java

include ../../../config.mk
include ../config.mk

java: $(CLASS_FILES) $(MAIN_CLASS)
	cd seq && make
	cd seq_exception && make
	cd no_exceptions && make java
	cd c-version && make

satin: java
	$(SATINC_COMMAND) $(MAIN_CLASS_NAME)
	cd seq && make
	cd seq_exception && make
	cd no_exceptions && make satin
	cd c-version && make

manta: $(MAIN) $(O_FILES)
	$(MANTAC_COMMAND) -o $(OUT) $(MAIN) $(O_FILES) ../../../../ibis/$(IBIS_LIB)
	cd seq && make
	cd seq_exception && make
	cd no_exceptions && make manta
	cd c-version && make

clean:
	rm -f $(TRASH_FILES)
	cd seq && make clean
	cd seq_exception && make clean
	cd no_exceptions && make clean
	cd c-version && make clean

include ../run.mk
