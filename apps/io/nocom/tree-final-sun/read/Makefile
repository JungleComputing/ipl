# SRC=  DowncallLatency.java

SRC	=
SRC	+= DITree.java
SRC	+= ByteArrayInputStream.java

MAIN = Main.java

OUT=latency

SERIALIZABLE	=
SERIALIZABLE	+= DITree.java

include ../../../../../config.mk

ifeq ($(PROFILE), 1)
OUT	:= $(OUT).gprof
endif

MANTACFLAGS	+= -S
MANTACFLAGS	+= -v
MANTACFLAGS	+= -inline
MANTACFLAGS	+= -show-inlining
# MANTACFLAGS	+= -no-opt-comm
# MANTACFLAGS	+= -pg

java: $(CLASS_FILES) $(MAIN_CLASS)
#	java ibis.frontend.io.IOGenerator DITree.class

clean:
	rm -f $(TRASH_FILES)
