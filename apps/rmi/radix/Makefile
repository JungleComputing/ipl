SRC	 = RMI_init.java
SRC	+= Barrier.java
SRC	+= IntRow.java
SRC	+= Job.java
SRC	+= Parts.java
SRC	+= PartsInterface.java
SRC	+= Prefix_Node.java
SRC	+= ProcArg.java
SRC	+= RadixMaster.java
SRC	+= RadixMasterInterface.java
SRC	+= RankArg.java
SRC	+= SlaveSort.java
SRC	+= Stats.java
SRC	+= Tree.java
SRC	+= TreeInterface.java

REMOTE	 = Parts.java
REMOTE	+= Tree.java
REMOTE	+= RadixMaster.java

SERIALIZABLE	 = IntRow.java
SERIALIZABLE	+= Job.java
SERIALIZABLE	+= Prefix_Node.java
SERIALIZABLE	+= ProcArg.java
SERIALIZABLE	+= RankArg.java
SERIALIZABLE	+= Stats.java

MAIN	 = Radix.java

# Rules are shared by all Makefiles

include ../../../config.mk

clean:
	rm -f $(TRASH_FILES) rmi_s*_Parts.java rmi_s*_Tree.java rmi_s*_RadixMaster.java
