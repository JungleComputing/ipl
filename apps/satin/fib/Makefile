APP_OPTIONS = 47

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

java: 
	$(JAVAC_COMMAND) $(MAIN)

satin:
	$(IBISC_COMMAND) -no-aborts $(MAIN)

INVOCATIONS	=
INVOCATIONS	+= Satin_Fib_fib_InvocationRecord.o

RETURNS		=
RETURNS		+= Satin_Fib_fib_ReturnRecord.o

O_FILES		+= $(INVOCATIONS) $(RETURNS)

INVOCATION_CLASS	= $(INVOCATIONS:%.o=%.class)
RETURN_CLASS		= $(RETURNS:%.o=%.class)

manta:	$(O_FILES) manta_ibisc
	# $(IBISC_COMMAND) -no-aborts -manta -o $(OUT) $(MAIN) -v

Satin_Fib_fib_InvocationRecord.class Satin_Fib_fib_ReturnRecord.class: Fib.class
	$(IOC_COMMAND) $<

Fib.class:	Fib.java
	$(JAVAC_COMMAND) $*.java
	$(SATINC_COMMAND) $*
	$(IOC_COMMAND) $*.class

FibInterface.class:	Fib.class
	$(SATINC_COMMAND) $*
	$(IOC_COMMAND) $*.class

manta_ibisc:	$(O_FILES) $(OUT)

test:
	../../../bin/ibis_nameserver -single &
	../../../bin/run_ibis 0 1 fib_test localhost Fib 30 -satin-stats -satin-closed > test_out 2> test_err
	grep "application result" test_out > test_res
	diff test_res test_goal

test_par:
	../../../bin/ibis_nameserver -single &
	rm -f test_par_out test_par_err test_par_res
	../../../bin/run_ibis 0 2 fib_par_test localhost Fib 30 $(PAR_TEST_OPTIONS) -satin-stats -satin-closed >> test_par_out 2>> test_par_err &
	../../../bin/run_ibis 1 2 fib_par_test localhost Fib 30 $(PAR_TEST_OPTIONS) -satin-stats -satin-closed >> test_par_out 2>> test_par_err
	grep "application result" test_par_out > test_par_res
	diff test_par_res test_goal

clean:
	rm -f $(TRASH_FILES) test_out test_err test_res test_par_out test_par_err test_par_res

include ../run.mk
