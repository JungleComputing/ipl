# SRC=  DowncallLatency.java

SRC	=
SRC	+= StoreOutputStream.java
SRC	+= StoreInputStream.java
SRC	+= StoreBuffer.java

MAIN = Main.java

OUT=latency

include ../../../../../config.mk

ifeq ($(PROFILE), 1)
OUT	:= $(OUT).gprof
endif

MANTACFLAGS	+= -S
MANTACFLAGS	+= -v
MANTACFLAGS	+= -g
# MANTACFLAGS	+= -no-opt-comm
# MANTACFLAGS	+= -keep

java: $(CLASS_FILES) $(MAIN_CLASS)

clean:
	rm -f $(TRASH_FILES)
