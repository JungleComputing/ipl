include ../../config.mk

java:
	cd call && $(MAKE) $(MFLAGS) java
	cd convert && $(MAKE) $(MFLAGS) java
	cd copy && $(MAKE) $(MFLAGS) java
	cd count && $(MAKE) $(MFLAGS) java
	cd file && $(MAKE) $(MFLAGS) java
	cd hash && $(MAKE) $(MFLAGS) java
	cd switch && $(MAKE) $(MFLAGS) java

manta:
	cd call && $(MAKE) $(MFLAGS) manta
	cd convert && $(MAKE) $(MFLAGS) manta
	cd copy && $(MAKE) $(MFLAGS) manta
	cd count && $(MAKE) $(MFLAGS) manta
	cd file && $(MAKE) $(MFLAGS) manta
	cd hash && $(MAKE) $(MFLAGS) manta
	cd switch && $(MAKE) $(MFLAGS) manta

clean:
	rm -f $(TRASH_FILES)
	cd call && $(MAKE) $(MFLAGS) clean
	cd convert && $(MAKE) $(MFLAGS) clean
	cd copy && $(MAKE) $(MFLAGS) clean
	cd count && $(MAKE) $(MFLAGS) clean
	cd file && $(MAKE) $(MFLAGS) clean
	cd hash && $(MAKE) $(MFLAGS) clean
	cd switch && $(MAKE) $(MFLAGS) clean
