include ../../config.mk

include config.mk

java:
	(for p in $(APPS); do \
		(cd $$p; \
		$(MAKE) $(MFLAGS) java) \
	done)

manta:
	(for p in $(APPS); do \
		(cd $$p; \
		$(MAKE) $(MFLAGS) manta) \
	done)

parrun:
	(for p in $(APPS); do \
		(cd $$p; \
		$(MAKE) $(MFLAGS) parrun) \
	done)

clean:
	rm -f $(TRASH_FILES)
	(for p in $(APPS); do \
		(cd $$p; \
		$(MAKE) $(MFLAGS) clean) \
	 done)

distclean:
	rm -f $(TRASH_FILES)
	(for p in $(APPS); do \
		(cd $$p; \
		$(MAKE) $(MFLAGS) distclean) \
	 done)
	cd benchmarks && $(MAKE) $(MFLAGS) distclean
	cd tp-JavaGrande02 && $(MAKE) $(MFLAGS) distclean
