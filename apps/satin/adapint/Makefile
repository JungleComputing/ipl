APP_OPTIONS = 0 3200000 0.000001
SMALL_APP_OPTIONS = 0 250000 0.000001
TEST_APP_OPTIONS = 0 64000 0.1

MAIN = AdapInt.java
OUT= adapint

include ../../../config.mk
include ../config.mk

satin:
	$(IBISC_COMMAND) $(MAIN)

clean:
	rm -f $(TRASH_FILES) test_out test_err test_res test_par_out test_par_err test_par_res

include ../run.mk
