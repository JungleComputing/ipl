include ../../../../config.mk

java:
	cd read_in_place && $(MAKE) $(MFLAGS) java
	cd read_new && $(MAKE) $(MFLAGS) java

manta:
	cd read_in_place && $(MAKE) $(MFLAGS) manta
	cd read_new && $(MAKE) $(MFLAGS) manta

clean:
	rm -f $(TRASH_FILES)
	cd read_in_place && $(MAKE) $(MFLAGS) clean
	cd read_new && $(MAKE) $(MFLAGS) clean
