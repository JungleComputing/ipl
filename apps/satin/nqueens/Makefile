APP_OPTIONS = 22

MAIN = NQueens.java
OUT= nqueens

include ../../../config.mk
include ../config.mk

java: 
	$(JAVAC_COMMAND) $(MAIN)
#	cd abort; make java
#	cd simple_abort; make java
#	cd exceptions; make java

satin: 
	$(IBISC_COMMAND) -no-aborts $(MAIN)
#	cd abort; make satin
#	cd simple_abort; make satin
#	cd exceptions; make satin

test:
	../../../bin/ibis_nameserver -single &
	../../../bin/run_ibis 0 1 nqueens_test localhost NQueens 13 -satin-closed > test_out 2> test_err
	grep "application result" test_out > test_res
	diff test_res test_goal

test_par:
	../../../bin/ibis_nameserver -single &
	rm -f test_par_out test_par_err test_par_res
	../../../bin/run_ibis 0 2 nqueens_par_test localhost NQueens 13 $(PAR_TEST_OPTIONS) -satin-stats -satin-closed >> test_par_out 2>> test_par_err &
	../../../bin/run_ibis 1 2 nqueens_par_test localhost NQueens 13 $(PAR_TEST_OPTIONS) -satin-stats -satin-closed >> test_par_out 2>> test_par_err
	grep "application result" test_par_out > test_par_res
	diff test_par_res test_goal

clean:
	rm -f $(TRASH_FILES) test_out test_err test_res test_par_out test_par_err test_par_res
	cd abort; make clean
	cd simple_abort; make clean
	cd exceptions; make clean

include ../run.mk
