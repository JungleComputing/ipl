SRC  = Test.java 
SRC += myGroup.java 

MAIN = Main.java
OUT=main

include ../../../config.mk

java: $(CLASS_FILES) $(MAIN_CLASS)
	rm -f group_*_Test.java
	$(GROUPC_COMMAND) Test
	$(JAVAC_COMMAND) group_*.java

clean:
	rm -f $(TRASH_FILES) group_*_Test.java
