SRC  = Barrier.java
SRC += ConstInterface.java
SRC += ForcesEnergy.java   
SRC += Input.java			    
SRC += Job.java		    
SRC += Molecule.java		    
SRC += MoleculeEnsemble.java		    
SRC += PredCorr.java		    
SRC += WaterMaster.java
SRC += WaterMasterInterface.java    
SRC += WaterWorker.java	    
SRC += WaterWorkerInterface.java    

MAIN = Water.java

REMOTE  = WaterMaster.java
REMOTE += WaterWorker.java

SERIALIZABLE  = Job.java
SERIALIZABLE += Molecule.java
SERIALIZABLE += MoleculeEnsemble.java

OUT=main

include ../../../config.mk

clean:
	rm -f $(TRASH_FILES) rmi_s*_WaterMaster.java rmi_s*_WaterWorker.java
