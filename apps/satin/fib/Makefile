APP_OPTIONS = 47
SMALL_APP_OPTIONS = 40
TEST_APP_OPTIONS = 30

TEST_IBIS=

MAIN		= Fib.java
INNER_CLASS	=
INNER_CLASS	+= FibInterface.class

# SATIN		=
# SATIN		+= Fib.java
# SATIN_METHODS	= Fib.fib

OUT= fib

MP_MODE		= panda
# MP_MODE		= mpi
MPI_MODE	= gm

MANTACFLAGS	+= -v

include ../../../config.mk
include ../config.mk

satin:
	$(IBISC_COMMAND) -no-aborts $(MAIN)

INVOCATIONS	=
INVOCATIONS	+= Satin_Fib_fib_InvocationRecord.o

RETURNS		=
RETURNS		+= Satin_Fib_fib_ReturnRecord.o

O_FILES		+= $(INVOCATIONS) $(RETURNS)

INVOCATION_CLASS	= $(INVOCATIONS:%.o=%.class)
RETURN_CLASS		= $(RETURNS:%.o=%.class)

SERIALIZABLE	= Satin_Fib_fib_InvocationRecord.class
SERIALIZABLE	+= Satin_Fib_fib_ReturnRecord.class

manta:	$(O_FILES) manta_ibisc
	# $(IBISC_COMMAND) -no-aborts -manta -o $(OUT) $(MAIN) -v

Fib.class:	Fib.java
	$(JAVAC_COMMAND) $*.java
	$(SATINC_COMMAND) $*
	$(IOC_COMMAND) $*.class

FibInterface.class:	Fib.class
	$(SATINC_COMMAND) $*
	$(IOC_COMMAND) $*.class

manta_ibisc:	$(O_FILES) $(OUT)

clean:
	rm -f $(TRASH_FILES) test_out test_err test_res test_par_out test_par_err test_par_res

include ../run.mk
