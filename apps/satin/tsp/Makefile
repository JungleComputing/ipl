APP_OPTIONS = 20 -bound 350
SMALL_APP_OPTIONS = 19 -bound 340
TEST_APP_OPTIONS = 11

MAIN = Tsp.java
OUT= tsp

# SERIALIZABLE	=
# SERIALIZABLE	+= DistanceTable.java
# SERIALIZABLE	+= Tsp.java
# SERIALIZABLE	+= Satin_Tsp_spawn_tsp_InvocationRecord.java
# SERIALIZABLE	+= Satin_Tsp_spawn_tsp_ReturnRecord.java

include ../../../config.mk
include ../config.mk

satin:
	$(IBISC_COMMAND) $(MAIN)

clean:
	rm -f $(TRASH_FILES) test_out test_err test_res test_par_out test_par_err test_par_res

include ../run.mk
