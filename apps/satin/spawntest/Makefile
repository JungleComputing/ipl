APP_OPTIONS =
SMALL_APP_OPTIONS =
TEST_APP_OPTIONS =

MAIN = SpawnTest.java
OUT= spawntest

include ../../../config.mk
include ../config.mk

satin:
	$(IBISC_COMMAND) -no-aborts $(MAIN)

clean:
	rm -f $(TRASH_FILES) test_out test_err test_res test_par_out test_par_err test_par_res

include ../run.mk
