APP_OPTIONS = 52

MAIN = FibThreshold.java
OUT= fib_threshold

include ../../../config.mk
include ../config.mk

satin:
	$(IBISC_COMMAND) -no-aborts $(MAIN)

test:
	../../../bin/ibis_nameserver -single &
	../../../bin/run_ibis 0 1 fib_threshold_test localhost FibThreshold 30 -satin-closed > test_out 2> test_err
	grep "application result" test_out > test_res
	diff test_res test_goal

test_par:
	../../../bin/ibis_nameserver -single &
	rm -f test_par_out test_par_err test_par_res
	../../../bin/run_ibis 0 2 fib_threshold_par_test localhost FibThreshold 30 $(PAR_TEST_OPTIONS) -satin-stats -satin-closed >> test_par_out 2>> test_par_err &
	../../../bin/run_ibis 1 2 fib_threshold_par_test localhost FibThreshold 30 $(PAR_TEST_OPTIONS) -satin-stats -satin-closed >> test_par_out 2>> test_par_err
	grep "application result" test_par_out > test_par_res
	diff test_par_res test_goal

clean:
	rm -f $(TRASH_FILES) test_out test_err test_res test_par_out test_par_err test_par_res

include ../run.mk
