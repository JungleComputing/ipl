APP_OPTIONS = 50

SRC  = FibThresholdInterface.java

MAIN = FibThreshold.java
OUT= fib_threshold

SERIALIZABLE	=
SERIALIZABLE	+= FibThreshold.java

include ../../../config.mk
include ../config.mk

java: $(CLASS_FILES) $(MAIN_CLASS)

satin: java
	$(SATINC_COMMAND) -no-aborts $(MAIN_CLASS_NAME)

clean:
	rm -f $(TRASH_FILES)

include ../run.mk
