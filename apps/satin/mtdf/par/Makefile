#APP_OPTIONS = 19
APP_OPTIONS = 17
TEST_APP_OPTIONS = 13

OUT= awari

MAIN=Mtdf.java

include ../../../../config.mk
include ../../config.mk

java:
	$(JAVAC_COMMAND) $(MAIN)

satin: 
	$(IBISC_COMMAND) -keep -irc-off $(MAIN)

do_test:
	../../../../bin/ibis_nameserver -no-poolserver -single -port $(NAMESERVER_PORT) &
	PRUN_ENV=test_par ../../../../bin/run_ibis 0 1 $(NAMESERVER_PORT) localhost $(MAIN_CLASS_NAME) $(TEST_APP_OPTIONS) -satin-stats -satin-tcp -satin-closed > test_out 2> test_err
	grep "application result" test_out > test_res
	diff test_res test_goal

do_test_par:
	../../../../bin/ibis_nameserver -no-poolserver -single -port $(NAMESERVER_PORT) &
	sleep 2
	rm -f test_par_out test_par_err test_par_res
	PRUN_ENV=test_par ../../../../bin/run_ibis 0 2 $(NAMESERVER_PORT) localhost $(MAIN_CLASS_NAME) $(TEST_APP_OPTIONS) $(PAR_TEST_OPTIONS) -satin-stats -satin-closed >> test_par_out 2>> test_par_err &
	PRUN_ENV=test_par ../../../../bin/run_ibis 1 2 $(NAMESERVER_PORT) localhost $(MAIN_CLASS_NAME) $(TEST_APP_OPTIONS) $(PAR_TEST_OPTIONS) -satin-stats -satin-closed >> test_par_out 2>> test_par_err
	grep "application result" test_par_out > test_par_res
	diff test_par_res test_goal

clean:
	rm -f $(TRASH_FILES) test_out test_err test_res test_par_out test_par_err test_par_res

include ../../run.mk
