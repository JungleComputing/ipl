APP_OPTIONS = -f inputs/1 -max 72
SMALL_APP_OPTIONS = -f inputs/2 -max 70
TEST_APP_OPTIONS = 54

MAIN = Ida.java
OUT= ida

include ../../../config.mk
include ../config.mk

satin:
	$(IBISC_COMMAND) -no-aborts $(MAIN)

clean:
	rm -f $(TRASH_FILES) test_out test_err test_res test_par_out test_par_err test_par_res

include ../run.mk
