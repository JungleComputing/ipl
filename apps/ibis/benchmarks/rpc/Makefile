SRC	 =
SRC	+= Data1.java
SRC	+= Data2.java
SRC	+= Latency.java

INNER_CLASS	 =
INNER_CLASS	+= RszHandler.class

MAIN = RPC.java

SERIALIZABLE	=
SERIALIZABLE	+= Data1.java
SERIALIZABLE	+= Data2.java

include ../../../../config.mk

RszHandler.class:	RPC.class

clean:
	rm -f $(TRASH_FILES)
