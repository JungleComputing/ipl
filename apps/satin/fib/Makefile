APP_OPTIONS = 47

OUT= fib

include ../../../config.mk
include ../config.mk

MAIN=Fib.java

satin:
	$(IBISC_COMMAND) -no-aborts $(MAIN)

manta:
	$(IBISC_COMMAND) -no-aborts -manta -o $(OUT) $(MAIN) -v


clean:
	rm -f $(TRASH_FILES)

include ../run.mk
