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

satin:
	(for p in $(APPS); do \
		(cd $$p; \
		$(MAKE) $(MFLAGS) satin) \
	done)

seqrun:
	(for p in $(APPS); do \
		(cd $$p; \
		$(MAKE) $(MFLAGS) seqrun) \
	done)

parrun:
	(for p in $(APPS); do \
		(cd $$p; \
		$(MAKE) $(MFLAGS) parrun) \
	done)

clean:
	rm -f $(TRASH_FILES)
	cd adapint; $(MAKE) $(MFLAGS) clean
	cd cover; $(MAKE) $(MFLAGS) clean
	cd fib; $(MAKE) $(MFLAGS) clean
	cd fib_threshold; $(MAKE) $(MFLAGS) clean
	cd ida; $(MAKE) $(MFLAGS) clean
	cd knapsack; $(MAKE) $(MFLAGS) clean
	cd mmult; $(MAKE) $(MFLAGS) clean
	cd noverk; $(MAKE) $(MFLAGS) clean
	cd nqueens; $(MAKE) $(MFLAGS) clean
	cd primfac; $(MAKE) $(MFLAGS) clean
	cd raytracer; $(MAKE) $(MFLAGS) clean
	cd tsp; $(MAKE) $(MFLAGS) clean
	cd two_out_three; $(MAKE) $(MFLAGS) clean
	cd two_out_three_non_static; $(MAKE) $(MFLAGS) clean
	cd paraffins; $(MAKE) $(MFLAGS) clean
	cd checkers; $(MAKE) $(MFLAGS) clean
	cd hello; $(MAKE) $(MFLAGS) clean
	cd abort_test; $(MAKE) $(MFLAGS) clean
	cd mtdf; $(MAKE) $(MFLAGS) clean
	cd lowlevel; $(MAKE) $(MFLAGS) clean

distclean:
	rm -f $(TRASH_FILES)
	cd adapint; $(MAKE) $(MFLAGS) distclean
	cd cover; $(MAKE) $(MFLAGS) distclean
	cd fib; $(MAKE) $(MFLAGS) distclean
	cd fib_threshold; $(MAKE) $(MFLAGS) distclean
	cd ida; $(MAKE) $(MFLAGS) distclean
	cd knapsack; $(MAKE) $(MFLAGS) distclean
	cd mmult; $(MAKE) $(MFLAGS) distclean
	cd noverk; $(MAKE) $(MFLAGS) distclean
	cd nqueens; $(MAKE) $(MFLAGS) distclean
	cd primfac; $(MAKE) $(MFLAGS) distclean
	cd raytracer; $(MAKE) $(MFLAGS) distclean
	cd tsp; $(MAKE) $(MFLAGS) distclean
	cd two_out_three; $(MAKE) $(MFLAGS) distclean
	cd two_out_three_non_static; $(MAKE) $(MFLAGS) distclean
	cd paraffins; $(MAKE) $(MFLAGS) distclean
	cd checkers; $(MAKE) $(MFLAGS) distclean
	cd hello; $(MAKE) $(MFLAGS) distclean
	cd abort_test; $(MAKE) $(MFLAGS) distclean
	cd mtdf; $(MAKE) $(MFLAGS) distclean
	cd lowlevel; $(MAKE) $(MFLAGS) distclean
