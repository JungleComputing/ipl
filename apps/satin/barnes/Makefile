APP_OPTIONS = 250000 10
SMALL_APP_OPTIONS = 400 20
TEST_APP_OPTIONS = -v 100 5
#TEST_APP_OPTIONS = 8 1

MAIN = BarnesHut.java
OUT= barnes

include ../../../config.mk
include ../config.mk

satin:
	$(IBISC_COMMAND) -no-aborts -keep $(MAIN)

clean:
	rm -f $(TRASH_FILES) test_out test_err test_res test_par_out test_par_err test_par_res

include ../run.mk
