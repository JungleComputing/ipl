SRC  = Abort.java

MAIN = NQueens.java
OUT= nqueens

SERIALIZABLE	=
SERIALIZABLE	+= Abort.java

include ../../../../config.mk

java: $(CLASS_FILES) $(MAIN_CLASS)

satin: java
	$(SATINC_COMMAND) $(MAIN_CLASS_NAME)

clean:
	rm -f $(TRASH_FILES)
