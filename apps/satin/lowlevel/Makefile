APP_OPTIONS = 44

SRC  = LowlevelInterface.java
SRC += DoneVoid.java
SRC += DoneInt.java
SRC += DoneObject.java
SRC += DoneVoidOverridden.java
SRC += DoneIntOverridden.java
SRC += DoneObjectOverridden.java

MAIN = Lowlevel.java
OUT= lowlevel

SERIALIZABLE	=
SERIALIZABLE	+= Lowlevel.java

include ../../../config.mk

include ../config.mk

CLASSPATH	= $(JAVACLASSPATH)
export CLASSPATH

java: $(CLASS_FILES) $(MAIN_CLASS)

satin: java
	$(SATINC_COMMAND) -keep $(MAIN_CLASS_NAME)

clean:
	rm -f $(TRASH_FILES)

include ../run.mk
