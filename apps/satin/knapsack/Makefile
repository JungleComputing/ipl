APP_OPTIONS = 32
SMALL_APP_OPTIONS = 29
TEST_APP_OPTIONS = 18

MAIN = Knapsack.java
OUT= knapsack

include ../../../config.mk
include ../config.mk

satin:
	$(IBISC_COMMAND) $(MAIN)

clean:
	rm -f $(TRASH_FILES) test_out test_err test_res test_par_out test_par_err test_par_res

include ../run.mk
