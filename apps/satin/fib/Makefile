APP_OPTIONS = 47
SMALL_APP_OPTIONS = 40
TEST_APP_OPTIONS = 30

TEST_IBIS=

MAIN		= Fib.java
OUT= fib

# SATIN		=
# SATIN		+= Fib.java
# SATIN_METHODS	= Fib.fib

MP_MODE		= panda
# MP_MODE		= mpi
MPI_MODE	= gm

MANTACFLAGS	+= -v

include ../../../config.mk
include ../config.mk

satin:
	$(IBISC_COMMAND) -no-aborts $(MAIN)

clean:
	rm -f $(TRASH_FILES) test_out test_err test_res test_par_out test_par_err test_par_res

include ../run.mk
