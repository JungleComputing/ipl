APP_OPTIONS = 33
SMALL_APP_OPTIONS = 30
TEST_APP_OPTIONS = 20

MAIN = Cover.java
OUT= cover

include ../../../config.mk
include ../config.mk

satin:
	$(IBISC_COMMAND) $(MAIN)

clean:
	rm -f $(TRASH_FILES) test_out test_err test_res test_par_out test_par_err test_par_res

include ../run.mk
