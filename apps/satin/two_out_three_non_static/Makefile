APP_OPTIONS = 44

SRC  = TwoOutThreeInterface.java
SRC += Done.java

MAIN = TwoOutThree.java
OUT= two_out_three

SERIALIZABLE	=
SERIALIZABLE	+= TwoOutThree.java

include ../../../config.mk

include ../config.mk

java: $(CLASS_FILES) $(MAIN_CLASS)

satin: java
	$(SATINC_COMMAND) $(MAIN_CLASS_NAME)

clean:
	rm -f $(TRASH_FILES)

include ../run.mk
