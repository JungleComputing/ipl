SRC  = Test.java 
SRC += myGroup.java 
SRC += Data.java 

MAIN = Main.java
OUT=main

SERIALIZABLE	=
SERIALIZABLE	+= Data.java

include ../../../config.mk

java: $(CLASS_FILES) $(MAIN_CLASS)
	rm -f group_*_Test.java group_*_myGroup.java
	$(GROUPC_COMMAND) Test myGroup
	$(JAVAC_COMMAND) group_*.java

clean:
	rm -f $(TRASH_FILES) group_*_Test.java group_*_myGroup.java
