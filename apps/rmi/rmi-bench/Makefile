# These rules describe the application.
#
# Define:
#   MAIN	= the .java file that contains the (outer) main()
#   RMISRC	= all .java files for remote classes
#   SRC		= all other .java files
#   EXEC	= name of the binary

SRC		=
SRC		+= Client.java
SRC		+= i_Server.java
SRC		+= Datatype.java
SRC		+= Int32.java
SRC		+= DITree.java
SRC		+= Cyclic.java
SRC		+= Worker.java
SRC		+= B.java
SRC		+= A.java
SRC		+= WithInner.java

MAIN		= RMI_Bench.java

REMOTE		=
REMOTE		+= Server.java

SERIALIZABLE	= Int32.java
SERIALIZABLE	+= DITree.java
SERIALIZABLE	+= Cyclic.java
SERIALIZABLE	+= B.java
SERIALIZABLE	+= WithInner.java
SERIALIZABLE	+= WithInner$$Inner.java

OUT		= RMI_Bench

# SRC		+= Escape.java

# Rules are shared by all Makefiles

include ../../../config.mk

clean:
	-rm -f $(TRASH_FILES)
