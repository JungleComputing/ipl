SRC  = Data.java
SRC += List.java
SRC += DList.java
SRC += Tree.java

MAIN = Main.java

SERIALIZABLE	=
SERIALIZABLE	+= Data.java
SERIALIZABLE	+= List.java
SERIALIZABLE	+= DList.java
SERIALIZABLE	+= Tree.java

INNER_CLASS	+= Receiver.class

include ../../../config.mk

Receiver.class:	Main.class

APP_OPTIONS =
APP_OPTIONS += -verbose
APP_OPTIONS += -array
APP_OPTIONS += -arraysize 102400
APP_OPTIONS += -count 1000
APP_OPTIONS += -panda

include ../config.mk

clean:
	rm -f $(TRASH_FILES)

include ../run.mk
