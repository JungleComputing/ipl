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

test:
	../../../bin/ibis_nameserver -single -port $(NAMESERVER_PORT) &
	PRUN_ENV=test_one_pool ../../../bin/run_ibis 0 1 $(NAMESERVER_PORT) localhost $(MAIN_CLASS_NAME) $(TEST_APP_OPTIONS) -satin-stats -satin-closed > test_out 2> test_err
	grep "application result" test_out > test_res
	diff test_res test_goal

test_par:
	../../../bin/ibis_nameserver -single -port $(NAMESERVER_PORT) &
	rm -f test_par_out test_par_err test_par_res
	PRUN_ENV=test_par_pool ../../../bin/run_ibis 0 2 $(NAMESERVER_PORT) localhost $(MAIN_CLASS_NAME) $(TEST_APP_OPTIONS) $(PAR_TEST_OPTIONS) -satin-stats -satin-closed >> test_par_out 2>> test_par_err &
	PRUN_ENV=test_par_pool ../../../bin/run_ibis 1 2 $(NAMESERVER_PORT) localhost $(MAIN_CLASS_NAME) $(TEST_APP_OPTIONS) $(PAR_TEST_OPTIONS) -satin-stats -satin-closed >> test_par_out 2>> test_par_err
	grep "application result" test_par_out > test_par_res
	diff test_par_res test_goal

logclean:
	rm -rf *.ps *~ out.plot *.dvi out.ps *.aux plots.log *.tmp out.log out.tex

distclean: clean logclean
	rm -rf output-*
	rm -rf satin_queue_log.*
	rm -rf log-output-*
