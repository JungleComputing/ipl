SRC	= DistanceTable.java
SRC	+= Input.java
SRC	+= Job.java
SRC	+= JobQueue.java
SRC	+= Minimum.java
SRC	+= MinimumReceiver.java
SRC	+= Misc.java
SRC	+= Client.java
SRC	+= RMI_init.java

REMOTE	=  MinimumImpl.java
REMOTE	+= JobQueueImpl.java
REMOTE	+= MinimumReceiverImpl.java

SERIALIZABLE	= Job.java

MAIN	=  Server.java
		
OUT	= tsp

# Rules are shared by all Makefiles

include ../../../config.mk

clean:
	rm -f $(TRASH_FILES) rmi_s*_MinimumImpl.java rmi_s*_JobQueueImpl.java rmi_s*_MinimumReceiverImpl.java
