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

MAIN		= RMI_Bench.java

REMOTE		=
REMOTE		+= Server.java

OUT		= RMI_Bench

# SRC		+= Escape.java

# Rules are shared by all Makefiles

include ../../../config.mk

# MANTAC_FLAGS	+= -S
# MANTAC_FLAGS	+= -link
# MANTAC_FLAGS	+= -no-opt_comm
# MANTAC_FLAGS	+= -pg -no-shared-link
MANTAC_FLAGS	+= -no-shared-link
# MANTAC_FLAGS	+= -hetero
MANTAC_FLAGS	+= -v
MANTAC_FLAGS	+= -keep
# MANTAC_FLAGS	+= -panda /usr/proj/panda/panda4.0
# MANTAC_LDFLAGS	+= -wa
# MANTAC_LDFLAGS	+= -sim
# MANTAC_LDFLAGS	+= -myrinet
# MANTAC_FLAGS	+= -shared-link
# MANTAC_FLAGS	:= $(filter-out -myrinet, $(MANTAC_FLAGS))

# MANTA_RMIC	:= $(filter-out -manta-escape, $(MANTA_RMIC))

clean:
	-rm -f $(TRASH_FILES)
