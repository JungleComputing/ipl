# SRC=  DowncallLatency.java

SRC	=

MAIN = Main.java

OUT=latency

include ../../../../../config.mk

ifeq ($(PROFILE), 1)
OUT	:= $(OUT).gprof
endif

MANTACFLAGS	+= -S
MANTACFLAGS	+= -v
# MANTACFLAGS	+= -no-opt-comm

java: $(CLASS_FILES) $(MAIN_CLASS)

clean:
	rm -f $(TRASH_FILES)
