APP_OPTIONS = 44

SRC  = FibInterface.java

MAIN = Fib.java
OUT= fib

include ../../../config.mk

include ../config.mk

CLASSPATH	= $(JAVACLASSPATH)
export CLASSPATH

java: $(CLASS_FILES) $(MAIN_CLASS)

satin: java
	$(SATINC_COMMAND) -no-aborts -keep $(MAIN_CLASS_NAME)

manta: $(MAIN) $(O_FILES)
	$(MANTAC_COMMAND) -o $(OUT) $(MAIN) $(O_FILES) $(MANTA_LIB)

mantapar: $(O_FILES) satin
	$(MANTAC_COMMAND) -c Satin_Fib_fib_InvocationRecord.java
	$(MANTAC_COMMAND) -c Satin_Fib_fib_ReturnRecord.java
	$(MANTAC_COMMAND) -o $(OUT) $(MAIN_CLASS) *.o $(MANTA_LIB)

satinser: java
	$(IOC_COMMAND) *.class
	make satin
	$(IOC_COMMAND) Satin_*.class

clean:
	rm -f $(TRASH_FILES)

include ../run.mk
