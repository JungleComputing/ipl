APP_OPTIONS = 44

SRC  = LowlevelInterface.java

MAIN = Lowlevel.java
OUT= lowlevel

include ../../../config.mk

include ../config.mk

CLASSPATH	= $(JAVACLASSPATH)
export CLASSPATH

java: $(CLASS_FILES) $(MAIN_CLASS)

satin: java
	$(SATINC_COMMAND) -no-aborts -keep $(MAIN_CLASS_NAME)

manta: $(MAIN) $(O_FILES)
	$(MANTAC_COMMAND) -o $(OUT) $(MAIN) $(O_FILES) $(MANTA_LIB)

#mantapar: $(O_FILES) satin
#	$(MANTAC_COMMAND) -c Satin_Fib_fib_InvocationRecord.java
#	$(MANTAC_COMMAND) -c Satin_Fib_fib_ReturnRecord.java
#	$(MANTAC_COMMAND) -o $(OUT) $(MAIN_CLASS) $(O_FILES) $(MANTA_LIB)

clean:
	rm -f $(TRASH_FILES)

include ../run.mk
