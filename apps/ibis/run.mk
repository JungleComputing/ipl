parrun:
	(for s in $(SIZES); do \
		( \
			echo --------------------------------------------------------------------------------; \
			echo running: "$(PAR_RUN_COMMAND)"; \
			$(PAR_RUN_COMMAND) \
		) \
	done) \

distclean: clean
	rm -rf output-*
