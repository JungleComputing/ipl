# SRC=  DowncallLatency.java

SRC	=
SRC	+= NullOutputStream.java
SRC	+= List.java

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
	java ibis.frontend.io.IOGenerator List.class

manta: $(MAIN) $(O_FILES)
#	$(MANTAC_COMMAND) -v -o $(OUT) $(MAIN) $(O_FILES) $(MANTA_LIB)

clean:
	rm -f $(TRASH_FILES)
