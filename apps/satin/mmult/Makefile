#for a matrix of 4096 x 4096
APP_OPTIONS = 5 0 128

#for a matrix of 1536x1536
#APP_OPTIONS = 5 0 48

#for a matrix of 1024x1024
#APP_OPTIONS = 4 0 64

SRC  = MmultInterface.java
SRC += Leaf.java
SRC += Matrix.java

MAIN = Mmult.java
OUT= mmult

SERIALIZABLE	=
SERIALIZABLE	+= Leaf.java
SERIALIZABLE	+= Matrix.java
SERIALIZABLE	+= Mmult.java

include ../../../config.mk
include ../config.mk

java: $(CLASS_FILES) $(MAIN_CLASS)

satin: java
	$(SATINC_COMMAND) -no-aborts $(MAIN_CLASS_NAME)
	$(IOC_COMMAND) Satin_*.class

clean:
	rm -f $(TRASH_FILES)

include ../run.mk
