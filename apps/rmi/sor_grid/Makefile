# These rules describe the application.
#
# Define:
#   MAIN	= the .java file that contains the (outer) main()
#   SRC		= all other .java files
#   RMISRC	= all .java files for remote classes
#   EXEC	= name of the binary

SRC		=
SRC		+= RMI_init.java
SRC		+= WaitingSendThread.java
SRC		+= i_GlobalData.java
SRC		+= i_SOR.java
SRC		+= GlobalData.java
SRC		+= Main.java
SRC		+= RemoteSORVisualizerInterface.java
SRC		+= SOR.java
SRC		+= SORCanvas.java
SRC		+= SORVisualizer.java


MAIN		= Main.java 

#REMOTE		+= SOR.java
#REMOTE		+= GlobalData .java

out		= sor


# Rules are shared by all Makefiles

include ../../../config.mk

rmic:
	rmic SOR GlobalData

doprint:
	@echo SRC = $(SRC)
	@echo CLASS_FILES = $(CLASS_FILES)
	@echo REMOTE = $(REMOTE)
	@echo REMOTE_STUBS = $(REMOTE_STUBS)

JAVACFLAGS	+= -g

clean:
	-rm -f $(TRASH_FILES)
