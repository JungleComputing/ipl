SRC	+= Data1.java
SRC	+= Data2.java

INNER_CLASS	+= RszHandler.class

MAIN = DowncallLatency.java
OUT=latency

SERIALIZABLE	=
SERIALIZABLE	+= Data1.java
SERIALIZABLE	+= Data2.java

# MP_MODE		= mpi
MP_MODE		= Panda
MPI_MODE	= gm
# MPI_MODE	= p4
# MPI_MODE	= panda

include ../../../../config.mk

RszHandler.class:	DowncallLatency.class

ifeq ($(PROFILE), 1)
OUT	:= $(OUT).gprof
endif

MANTACFLAGS	+= -S
MANTACFLAGS	+= -v
# MANTACFLAGS	+= -no-opt-comm
ifeq (1, $(OPTIMIZE))
MANTACFLAGS	+= -inline
endif

# Only for .class compiler O_FILES		+= RszHandler.o

# java: $(CLASS_FILES) $(MAIN_CLASS)

clean:
	rm -f $(TRASH_FILES)
