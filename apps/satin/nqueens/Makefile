APP_OPTIONS = 22
SMALL_APP_OPTIONS = 20
TEST_APP_OPTIONS = 13

MAIN = NQueens.java
OUT= nqueens

include ../../../config.mk
include ../config.mk

java: 
	$(JAVAC_COMMAND) $(MAIN)
#	cd abort && make java
#	cd simple_abort && make java
#	cd exceptions && make java

satin: 
	$(IBISC_COMMAND) -no-aborts $(MAIN)
#	cd abort && make satin
#	cd simple_abort && make satin
#	cd exceptions && make satin

clean:
	rm -f $(TRASH_FILES) test_out test_err test_res test_par_out test_par_err test_par_res
	cd abort && make clean
	cd simple_abort && make clean
	cd exceptions && make clean

include ../run.mk
