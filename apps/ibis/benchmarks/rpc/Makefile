SRC	+= Data1.java
SRC	+= Data2.java

MAIN = DowncallLatency.java
OUT=latency

SERIALIZABLE	=
SERIALIZABLE	+= Data1.java
SERIALIZABLE	+= Data2.java

include ../../../../config.mk

ifeq ($(PROFILE), 1)
OUT	:= $(OUT).gprof
endif

MANTACFLAGS	+= -S
# MANTACFLAGS	+= -v
# MANTACFLAGS	+= -no-opt-comm

# Only for .class compiler O_FILES		+= RszHandler.o

# java: $(CLASS_FILES) $(MAIN_CLASS)

manta:
	@echo Don't make a manta exec here\!
	cd manta; $(MAKE) $(MFLAGS)

clean:
	rm -f $(TRASH_FILES)
	-cd manta; $(MAKE) $(MFLAGS) clean
