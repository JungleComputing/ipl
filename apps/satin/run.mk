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

csim:
	if [ -z "$(CLUSTERS)" ]; then echo CLUSTERS not set; exit 1; fi
	if [ -z "$(NODES)" ]; then echo NODES not set; exit 1; fi
	if [ -z "$(SPIN)" ]; then echo SPIN not set; exit 1; fi
	if [ -z "$(THRP)" ]; then echo THRP not set; exit 1; fi
	../csim.sh $(CLUSTERS) $(NODES) $(SPIN) $(THRP) $(MAIN_CLASS_NAME) $(APP_OPTIONS)

csimtest:
	if [ -z "$(CLUSTERS)" ]; then echo CLUSTERS not set; exit 1; fi
	if [ -z "$(NODES)" ]; then echo NODES not set; exit 1; fi
	../csim.sh $(CLUSTERS) $(NODES) 0.000001 1048576 $(MAIN_CLASS_NAME) $(TEST_APP_OPTIONS)

csimtcp:
	if [ -z "$(CLUSTERS)" ]; then echo CLUSTERS not set; exit 1; fi
	if [ -z "$(NODES)" ]; then echo NODES not set; exit 1; fi
	../csim-tcp.sh $(CLUSTERS) $(NODES) $(NAMESERVER_PORT) $(NAME_SERVER) $(MAIN_CLASS_NAME) $(TEST_APP_OPTIONS)

pprun:
	if [ -z "$(SITES)" ]; then echo SITES not set; exit 1; fi
	../../../globus/pprun $(NAMESERVER_PORT) $(SITES) - $(MAIN_CLASS_NAME) $(APP_OPTIONS) -satin-stats -satin-closed

pprun_test:
	if [ -z "$(SITES)" ]; then echo SITES not set; exit 1; fi
	../../../globus/pprun $(NAMESERVER_PORT) $(SITES) - $(MAIN_CLASS_NAME) $(TEST_APP_OPTIONS) -satin-stats -satin-closed

grun:
	if [ -z "$(SITES)" ]; then echo SITES not set; exit 1; fi
	../../../globus/grun $(NAMESERVER_PORT) $(SITES) - $(MAIN_CLASS_NAME) $(APP_OPTIONS) -satin-stats -satin-closed

grun_test:
	if [ -z "$(SITES)" ]; then echo SITES not set; exit 1; fi
	../../../globus/grun $(NAMESERVER_PORT) $(SITES) - $(MAIN_CLASS_NAME) $(TEST_APP_OPTIONS) -satin-stats -satin-closed

logclean:
	rm -rf *.ps *~ out.plot *.dvi out.ps *.aux plots.log *.tmp out.log out.tex

distclean: clean logclean
	rm -rf output-*
	rm -rf satin_queue_log.*
	rm -rf log-output-*
