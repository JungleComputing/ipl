APP_OPTIONS = 39
SMALL_APP_OPTIONS = 37
TEST_APP_OPTIONS = 28

MAIN = NoverK.java
OUT= noverk

include ../../../config.mk
include ../config.mk

satin:
	$(IBISC_COMMAND) $(MAIN)

clean:
	rm -f $(TRASH_FILES) test_out test_err test_res test_par_out test_par_err test_par_res

include ../run.mk
