APP_OPTIONS = 21

TEST_IBIS=

OUT= awari

include ../../../../config.mk
include ../../config.mk

MAIN=Main.java

java: 
	$(JAVAC_COMMAND) $(MAIN)

satin:
	$(IBISC_COMMAND) $(MAIN)

manta: $(OBJ)
	$(MANTAC)    $(MANTA_FLAGS) $(MAIN) -o $(EXEC) *.o

clean:
	rm -f $(TRASH_FILES) test_out test_err test_res test_par_out test_par_err test_par_res

include ../../run.mk
