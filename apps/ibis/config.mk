
APPS =
APPS += tp-JavaGrande02

SIZES = 2

PAR_OUT_FILE = output-$(OUT)-par-$$s

PAR_TIME = -asocial -t 2:0:0

EXCLUDES =

NAME_SERVER = fs0.das2.cs.vu.nl

PAR_POOL_NAME = $(OUT)-par-$$s

DATE = `date '+%Y-%m-%d'`

# don't forget -server flag for the SUN JIT
#JVM_FLAGS = -server


SEQ_RUN_COMMAND = (/home/rob/bin/check_rsh_ports; rm -f $(SEQ_OUT_FILE) $(SEQ_OUT_FILE).0; prun $(EXCLUDES) -1 -o $(SEQ_OUT_FILE) $(SEQ_TIME) -v $(IBIS_ROOT)/ibis/bin/run_ibis 1 $(SEQ_POOL_NAME) $(NAME_SERVER) $(JVM_FLAGS) $(MAIN_CLASS_NAME) $(APP_OPTIONS) $(SEQ_RUN_OPTIONS) < /dev/null; mv $(SEQ_OUT_FILE).0 $(SEQ_OUT_FILE)) &

PAR_RUN_COMMAND = (/home/rob/bin/check_rsh_ports; rm -f $(PAR_OUT_FILE) $(PAR_OUT_FILE).*; prun $(EXCLUDES) -1 -o $(PAR_OUT_FILE) $(PAR_TIME) -v $(IBIS_ROOT)/ibis/bin/run_ibis $$s $(PAR_POOL_NAME) $(NAME_SERVER) $(JVM_FLAGS) $(MAIN_CLASS_NAME) $(APP_OPTIONS) $(PAR_RUN_OPTIONS) < /dev/null; cat $(PAR_OUT_FILE).* > $(PAR_OUT_FILE); rm -f $(PAR_OUT_FILE).*)
