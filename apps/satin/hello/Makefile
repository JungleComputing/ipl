SRC  = HelloInterface.java

MAIN = Hello.java
OUT= hello

SERIALIZABLE	=
SERIALIZABLE	+= Hello.java

include ../../../config.mk

java: $(CLASS_FILES) $(MAIN_CLASS)

satin: java
	$(SATINC_COMMAND) $(MAIN_CLASS_NAME)

clean:
	rm -f $(TRASH_FILES)
