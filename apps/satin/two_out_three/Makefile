APP_OPTIONS = 

MAIN = TwoOutThree.java
OUT= two_out_three

include ../../../config.mk
include ../config.mk

java: 
	$(JAVAC_COMMAND) $(MAIN)

satin:
	$(IBISC_COMMAND) $(MAIN)

test:
	../../../bin/ibis_nameserver -single &
	../../../bin/run_ibis 0 1 two_out_three localhost TwoOutThree -satin-closed > test_out 2> test_err
	grep "application result" test_out > test_res
	diff test_res test_goal

clean:
	rm -f $(TRASH_FILES) test_out test_err test_res

include ../run.mk
