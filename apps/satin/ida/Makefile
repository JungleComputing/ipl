APP_OPTIONS = 90

SRC  = Game.java
SRC += Job.java
SRC += Position.java

MAIN = Ida.java
OUT= ida

SERIALIZABLE	=
SERIALIZABLE	+= Ida.java
SERIALIZABLE	+= Game.java
SERIALIZABLE	+= Job.java
SERIALIZABLE	+= Position.java

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
