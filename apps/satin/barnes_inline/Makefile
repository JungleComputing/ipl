APP_OPTIONS = -tuple2 -t 10 2000000 200
SMALL_APP_OPTIONS = 400 20
TEST_APP_OPTIONS = -tuple2 -v 100 5
#TEST_APP_OPTIONS = 8 1

MAIN = BarnesHut.java
OUT= barnesInline

include ../../../config.mk
include ../config.mk

satin:
	$(IBISC_COMMAND) -keep $(MAIN)

clean:
	rm -f $(TRASH_FILES) test_out test_err test_res test_par_out test_par_err test_par_res

include ../run.mk
