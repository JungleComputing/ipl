include ../../../../config.mk

java: 
	cd read && $(MAKE) $(MFLAGS) java
	cd write && $(MAKE) $(MFLAGS) java

manta:
	cd read && $(MAKE) $(MFLAGS) manta
	cd write && $(MAKE) $(MFLAGS) manta

clean:
	rm -f $(TRASH_FILES)
	cd read && $(MAKE) $(MFLAGS) clean
	cd write && $(MAKE) $(MFLAGS) clean
