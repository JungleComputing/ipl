include ../../../config.mk

java:
	cd arrays && $(MAKE) $(MFLAGS) java
	cd list && $(MAKE) $(MFLAGS) java
	cd object && $(MAKE) $(MFLAGS) java
	cd tree && $(MAKE) $(MFLAGS) java

manta:
	cd arrays && $(MAKE) $(MFLAGS) manta
	cd list && $(MAKE) $(MFLAGS) manta
	cd object && $(MAKE) $(MFLAGS) manta
	cd tree && $(MAKE) $(MFLAGS) manta

clean:
	rm -f $(TRASH_FILES)
	cd arrays && $(MAKE) $(MFLAGS) clean
	cd list && $(MAKE) $(MFLAGS) clean
	cd object && $(MAKE) $(MFLAGS) clean
	cd tree && $(MAKE) $(MFLAGS) clean
