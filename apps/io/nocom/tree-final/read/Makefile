# SRC=  DowncallLatency.java

SRC	=
SRC	+= StoreArrayOutputStream.java
SRC	+= StoreArrayInputStream.java
SRC	+= StoreBuffer.java
SRC	+= DITree.java

MAIN = Main.java

OUT=latency

SERIALIZABLE	=
SERIALIZABLE	+= DITree.java

include ../../../../../config.mk

ifeq ($(PROFILE), 1)
OUT	:= $(OUT).gprof
endif

MANTACFLAGS	+= -Si
MANTACFLAGS	+= -S
MANTACFLAGS	+= -v
MANTACFLAGS	+= -inline
MANTACFLAGS	+= -show_inlining
# MANTACFLAGS	+= -no-opt-comm

java: $(CLASS_FILES) $(MAIN_CLASS)
	java ibis.frontend.io.IOGenerator DITree.class

clean:
	rm -f $(TRASH_FILES)
