#for a matrix of 4096 x 4096
APP_OPTIONS = 5 0 128

#for a matrix of 4096 x 4096
SMALL_APP_OPTIONS = 5 0 96

#for a matrix of 1536x1536
#APP_OPTIONS = 5 0 48

#for a matrix of 1024x1024
#APP_OPTIONS = 4 0 64

TEST_APP_OPTIONS = 4 0 32

SRC	=
SRC	+= Leaf.java
SRC	+= Matrix.java
SRC	+= MmultInterface.java

MAIN = Mmult.java
OUT= mmult

include ../../../config.mk
include ../config.mk

satin:
	$(IBISC_COMMAND) -no-aborts $(MAIN)

clean:
	rm -f $(TRASH_FILES) test_out test_err test_res test_par_out test_par_err test_par_res

include ../run.mk
