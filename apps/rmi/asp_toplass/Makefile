# These rules describe the application.
#
# Define:
#   MAIN	= the .java file that contains the (outer) main()
#   RMISRC	= all .java files for remote classes
#   SRC		= all other .java files
#   EXEC	= name of the binary

SRC		=  BtreePoolSender.java 
SRC		+= BtreeSender.java 
SRC		+= OrcaRandom.java
SRC		+= i_Asp.java
SRC		+= i_GlobalData.java
SRC		+= RMI_init.java

MAIN		=  Main.java

REMOTE		=  Asp.java
REMOTE		+= GlobalData.java

EXEC		= asp


# Rules are shared by all Makefiles

include ../../../config.mk

# MANTAC_FLAGS	+= -no-opt_comm
# MANTAC_FLAGS	+= -pg -no-shared-link
# MANTAC_FLAGS	+= -no-shared-link

clean:
	-rm -f $(TRASH_FILES)
