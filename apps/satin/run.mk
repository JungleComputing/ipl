seqrun:
	echo --------------------------------------------------------------------------------
	echo running: "$(SEQ_RUN_COMMAND)"
	$(SEQ_RUN_COMMAND)

3seqrun:
	make VERS=1 seqrun
	make VERS=2 seqrun
	make VERS=3 seqrun


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

3parrun:
	make VERS=1 parrun
	make VERS=2 parrun
	make VERS=3 parrun

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

test_java:
	PRUN_ENV=foo ../../../bin/run_ibis 0 1 foo bar $(MAIN_CLASS_NAME) $(TEST_APP_OPTIONS) > test_out 2> test_err
	grep "application result" test_out > test_res
	diff test_res test_goal

test:
	../../../bin/ibis_nameserver -single -port $(NAMESERVER_PORT) &
	sleep 1
	PRUN_ENV=test_one_pool USE_JAVA_WRAPPER= ../../../bin/run_ibis 0 1 $(NAMESERVER_PORT) localhost $(MAIN_CLASS_NAME) $(TEST_APP_OPTIONS) -satin-tcp -satin-stats -satin-closed > test_out 2> test_err
	grep "application result" test_out > test_res
	diff test_res test_goal

test_par:
	../../../bin/ibis_nameserver -single -port $(NAMESERVER_PORT) &
	sleep 2
	rm -f test_par_out.[01] test_par_err.[01] test_par_res
	PRUN_ENV=test_par_pool USE_JAVA_WRAPPER= ../../../bin/run_ibis 0 2 $(NAMESERVER_PORT) localhost $(MAIN_CLASS_NAME) $(TEST_APP_OPTIONS) $(PAR_TEST_OPTIONS) -satin-stats -satin-closed >> test_par_out.0 2>> test_par_err.0 &
	PRUN_ENV=test_par_pool USE_JAVA_WRAPPER= ../../../bin/run_ibis 1 2 $(NAMESERVER_PORT) localhost $(MAIN_CLASS_NAME) $(TEST_APP_OPTIONS) $(PAR_TEST_OPTIONS) -satin-stats -satin-closed >> test_par_out.1 2>> test_par_err.1
	grep "application result" test_par_out.[01] > test_par_res
	diff test_par_res test_goal

PANDA_SATIN_PARAMS=-satin-closed -satin-stats -satin-panda -satin-ibis -satin-alg $(ALG)

panda_test:
	make ALG=CRS PANDA_APP_OPTIONS=$(TEST_APP_OPTIONS) panda_runner

panda_small:
	make PANDA_APP_OPTIONS=$(SMALL_APP_OPTIONS) panda_runner

panda_run:
	make PANDA_APP_OPTIONS=$(APP_OPTIONS) panda_runner

panda_runner:
	if [ -z "$(NODES)" ]; then echo NODES not set; exit 1; fi
	if [ -z "$(ALG)" ]; then echo ALG not set; exit 1; fi
	if [ -z "$(PANDA_APP_OPTIONS)" ]; then echo PANDA_APP_OPTIONS not set; exit 1; fi
	prun -v -1 -o pandarun$(ID)$(ALG)$(NODES) ../../../bin/run_ibis $(NODES) foo bar $(MAIN_CLASS_NAME) $(PANDA_APP_OPTIONS) $(PANDA_SATIN_PARAMS)
	beep; sleep 0.3; beep

panda_runs:
	make ALG=RS ID=0 panda_run
	make ALG=RS ID=1 panda_run
	#make ALG=CRS ID=0 panda_run
	#make ALG=CRS ID=1 panda_run

PPRUN_PARAMS=$(NAMESERVER_PORT) $(SITES) - $(MAIN_CLASS_NAME)
PPRUN_SATIN_PARAMS=-satin-stats -satin-closed -satin-ibis -satin-alg $(ALG)
pprun_test:
	make PPRUN_APP_OPTS="$(TEST_APP_OPTIONS)" pprunner

pprun_small:
	make PPRUN_APP_OPTS="$(SMALL_APP_OPTIONS)" pprunner

pprun:
	make PPRUN_APP_OPTS="$(APP_OPTIONS)" pprunner

pprunner:
	if [ -z "$(SITES)" ]; then echo SITES not set; exit 1; fi
	if [ -z "$(ALG)" ]; then echo ALG not set; exit 1; fi
	../../../bin/pprun $(PPRUN_PARAMS) $(PPRUN_APP_OPTS)  $(PPRUN_SATIN_PARAMS)

GRUN_SATIN_PARAMS=-satin-stats -satin-closed -satin-alg $(ALG)
grun_test:
	make GRUN_APP_OPTS="$(TEST_APP_OPTIONS)" grunner

grun_small:
	make GRUN_APP_OPTS="$(SMALL_APP_OPTIONS)" grunner

grun:
	make GRUN_APP_OPTS="$(APP_OPTIONS)" grunner

grunner:
	if [ -z "$(SITES)" ]; then echo SITES not set; exit 1; fi
	if [ -z "$(ALG)" ]; then echo ALG not set; exit 1; fi
	../../../bin/grun $(NAMESERVER_PORT) $(SITES) - $(MAIN_CLASS_NAME) $(GRUN_APP_OPTS) $(GRUN_SATIN_PARAMS)

logclean:
	rm -rf *.ps *~ out.plot *.dvi out.ps *.aux plots.log *.tmp out.log out.tex

distclean: clean logclean
	rm -rf output-*
	rm -rf satin_queue_log.*
	rm -rf log-output-*
