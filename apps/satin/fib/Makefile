APP_OPTIONS = 44

OUT= fib

include ../../../config.mk
include ../config.mk

java:
	$(JAVAC_COMMAND) Fib.java

satin: 
	$(IBISC_COMMAND) -no-aborts -keep Fib.java

clean:
	rm -f $(TRASH_FILES)

include ../run.mk
