
APPS =
#APPS += hello
#APPS += abort_test
#APPS += lowlevel
APPS += fib
APPS += tsp 
APPS += ida
APPS += fib_threshold
APPS += knapsack
APPS += nqueens
APPS += cover
APPS += noverk
APPS += primfac
APPS += adapint
APPS += raytracer
APPS += mmult
#APPS += two_out_three
#APPS += two_out_three_non_static
#APPS += checkers

#SIZES = 1 2 4 8 16 24 32 48 64
#SIZES = 1 2 4 16 24 32 48 64
SIZES = 1 2 4 8

PAR_SCHEDULING  =
PAR_SCHEDULING += RS

SEQ_OUT_FILE = output-$(OUT)-seq
PAR_OUT_FILE = output-$(OUT)-par-$$q-$$s

SEQ_RUN_OPTIONS =
PAR_RUN_OPTIONS  = -satin-stats 
PAR_RUN_OPTIONS += -satin-closed
# PAR_RUN_OPTIONS += -satin-manta

SEQ_TIME = -asocial -t 5:0:0
PAR_TIME = -asocial -t 2:0:0

EXCLUDES =

NAME_SERVER = fs0.das2.cs.vu.nl

PAR_POOL_NAME = $(OUT)-par-$$q-$$s
SEQ_POOL_NAME = $(OUT)-seq

DATE = `date '+%Y-%m-%d'`

# don't forget -server flag for the SUN JIT
#JVM_FLAGS = -server


SEQ_RUN_COMMAND = (/home/rob/bin/check_rsh_ports; echo "PATH=$$PATH"; rm -f $(SEQ_OUT_FILE) $(SEQ_OUT_FILE).0; prun $(EXCLUDES) -1 -o $(SEQ_OUT_FILE) $(SEQ_TIME) -v $(IBIS_ROOT)/ibis/bin/run_ibis 1 $(SEQ_POOL_NAME) $(NAME_SERVER) $(JVM_FLAGS) $(MAIN_CLASS_NAME) $(APP_OPTIONS) $(SEQ_RUN_OPTIONS) < /dev/null; mv $(SEQ_OUT_FILE).0 $(SEQ_OUT_FILE)) &

PAR_RUN_COMMAND = (/home/rob/bin/check_rsh_ports; rm -f $(PAR_OUT_FILE) $(PAR_OUT_FILE).*; prun $(EXCLUDES) -1 -o $(PAR_OUT_FILE) $(PAR_TIME) -v $(IBIS_ROOT)/ibis/bin/run_ibis $$s $(PAR_POOL_NAME) $(NAME_SERVER) $(JVM_FLAGS) $(MAIN_CLASS_NAME) $(APP_OPTIONS) $(PAR_RUN_OPTIONS) < /dev/null; cat $(PAR_OUT_FILE).* > $(PAR_OUT_FILE); rm -f $(PAR_OUT_FILE).*)
