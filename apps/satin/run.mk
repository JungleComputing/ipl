seqrun:
	echo --------------------------------------------------------------------------------
	echo running: "$(SEQ_RUN_COMMAND)"
	$(SEQ_RUN_COMMAND)


parrun:
	(for q in $(PAR_SCHEDULING); do \
		(for s in $(SIZES); do \
			( \
				echo --------------------------------------------------------------------------------; \
				echo running: "$(PAR_RUN_COMMAND)"; \
				$(PAR_RUN_COMMAND) \
			) \
		done) \
	done)


simrun:
	(for q in $(SIM_SCHEDULING); do \
		(for s in $(SIM_SIZES); do \
			( \
				(for l in $(SPINS); do \
					( \
						(for t in $(THROUGHPUTS); do \
							( \
								echo --------------------------------------------------------------------------------; \
								echo running: "$(SIM_RUN_COMMAND)"; \
								$(SIM_RUN_COMMAND) \
							) \
						done) \
					) \
				done) \
			) \
		done) \
	done)


parlogs:
	(for q in $(PAR_SCHEDULING); do \
		(for s in $(SIZES); do \
			( \
				../logs/do_log_plot $(PAR_LOGGING_FILE) \
			) \
		done) \
	done)


simlogs:
	(for s in $(SIM_SIZES); do \
		( \
			(for l in $(SPINS); do \
				( \
					(for t in $(THROUGHPUTS); do \
						( \
							../logs/do_log_plot $(SIM_LOGGING_FILE) \
						) \
					done) \
				) \
			done) \
		) \
	done)

logclean:
	rm -rf *.ps *~ out.plot *.dvi out.ps *.aux plots.log *.tmp out.log out.tex

distclean: clean logclean
	rm -rf output-*
	rm -rf satin_queue_log.*
	rm -rf log-output-*
