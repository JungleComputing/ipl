include ../../../../config.mk

java:
	cd final && $(MAKE) $(MFLAGS) java
	cd normal && $(MAKE) $(MFLAGS) java

manta:
	cd final && $(MAKE) $(MFLAGS) manta
	cd normal && $(MAKE) $(MFLAGS) manta

clean:
	rm -f $(TRASH_FILES)
	cd final && $(MAKE) $(MFLAGS) clean
	cd normal && $(MAKE) $(MFLAGS) clean
