APP_OPTIONS = 36

SRC  = ParaffinsInterface.java

MAIN = Paraffins.java
OUT= paraffins

SERIALIZABLE	=
SERIALIZABLE	+= Paraffins.java

include ../../../config.mk
include ../config.mk

java: $(CLASS_FILES) $(MAIN_CLASS)

satin: java
	$(SATINC_COMMAND) $(MAIN_CLASS_NAME)

clean:
	rm -f $(TRASH_FILES)

include ../run.mk
