SRC	 = Barrier.java
SRC	+= Master.java
SRC	+= MasterInterface.java
SRC	+= Matrix.java
SRC	+= RMI_init.java
SRC	+= Slave.java
SRC	+= SlaveInterface.java

REMOTE	 = Master.java
REMOTE	+= Slave.java

MAIN	= fft.java
		
OUT	= fft

# Rules are shared by all Makefiles

include ../../../config.mk

clean:
	rm -f $(TRASH_FILES) rmi_s*_Master.java rmi_s*_Slave.java
