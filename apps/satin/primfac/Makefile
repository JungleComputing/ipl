APP_OPTIONS = 19678904321
SMALL_APP_OPTIONS = 6678904321
TEST_APP_OPTIONS = 19678904

MAIN = PrimFac.java
OUT= primfac

include ../../../config.mk
include ../config.mk

java:
	$(JAVAC_COMMAND) $(MAIN)

satin:
	$(IBISC_COMMAND) $(MAIN)

clean:
	rm -f $(TRASH_FILES) test_out test_err test_res test_par_out test_par_err test_par_res

include ../run.mk
