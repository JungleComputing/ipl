include ../../config.mk

java: 
	cd benchmarks; $(MAKE) $(MFLAGS) java
	cd tp-JavaGrande02; $(MAKE) $(MFLAGS) java

manta:
	cd benchmarks; $(MAKE) $(MFLAGS) manta
	cd tp-JavaGrande02; $(MAKE) $(MFLAGS) manta

clean:
	rm -f $(TRASH_FILES)
	cd benchmarks; $(MAKE) $(MFLAGS) clean
	cd tp-JavaGrande02; $(MAKE) $(MFLAGS) clean
