APP_OPTIONS = 44

MAIN = AbortTest.java
OUT = abort_test

SERIALIZABLE	=
SERIALIZABLE	+= AbortTest.java

include ../../../config.mk

include ../config.mk

java: $(CLASS_FILES) $(MAIN_CLASS)

satin: java
	$(SATINC_COMMAND) $(MAIN_CLASS_NAME)

clean:
	rm -f $(TRASH_FILES)

include ../run.mk
