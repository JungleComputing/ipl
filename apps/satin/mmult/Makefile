#for a matrix of 4096 x 4096
APP_OPTIONS = 5 0 128

#for a matrix of 1536x1536
#APP_OPTIONS = 5 0 48

#for a matrix of 1024x1024
#APP_OPTIONS = 4 0 64

SRC	=
SRC	+= Leaf.java
SRC	+= Matrix.java
SRC	+= MmultInterface.java

MAIN = Mmult.java
OUT= mmult

include ../../../config.mk
include ../config.mk

satin:
	$(IBISC_COMMAND) -no-aborts $(MAIN)

test:
	../../../bin/ibis_nameserver -single &
	../../../bin/run_ibis 0 1 mmult_test localhost Mmult 4 0 32 -satin-closed > test_out 2> test_err
	grep "application result" test_out > test_res
	diff test_res test_goal

test_par:
	../../../bin/ibis_nameserver -single &
	rm -f test_par_out test_par_err test_par_res
	../../../bin/run_ibis 0 2 mmult_par_test localhost Mmult 4 0 32 $(PAR_TEST_OPTIONS) -satin-stats -satin-closed >> test_par_out 2>> test_par_err &
	../../../bin/run_ibis 1 2 mmult_par_test localhost Mmult 4 0 32 $(PAR_TEST_OPTIONS) -satin-stats -satin-closed >> test_par_out 2>> test_par_err
	grep "application result" test_par_out > test_par_res
	diff test_par_res test_goal

clean:
	rm -f $(TRASH_FILES) test_out test_err test_res test_par_out test_par_err test_par_res

include ../run.mk
