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

RUNJAVA = /home3/rob/bin/runjava_ibis_ibm
SIZES = 64 48 32 24 16 8 4 2 1

PAR_SCHEDULING  =
PAR_SCHEDULING += RS

SEQ_OUT_FILE = output-$(OUT)-seq
PAR_OUT_FILE = output-$(OUT)-par-$$q-$$s

SEQ_RUN_OPTIONS =
PAR_RUN_OPTIONS = -satin-stats -satin-closed

SEQ_TIME = -asocial -t 3:0:0
PAR_TIME = -asocial -t 3:0:0

EXCLUDES =

DATE = `date '+%Y-%m-%d'`

# becasue of the temp file, do not run make parrun in the background!
SEQ_RUN_COMMAND = (home/rob/bin/check_rsh_ports; rm -rf .ibis_pool_name $(SEQ_OUT_FILE); prun $(EXCLUDES) -1 -o $(SEQ_OUT_FILE) $(SEQ_TIME) -v $(RUNJAVA) 1 $(MAIN_CLASS_NAME) $(APP_OPTIONS) $(SEQ_RUN_OPTIONS) < /dev/null; mv $(SEQ_OUT_FILE).0 $(SEQ_OUT_FILE)) &

PAR_RUN_COMMAND = (/home/rob/bin/check_rsh_ports; rm -f .ibis_pool_name $(PAR_OUT_FILE).*; prun $(EXCLUDES) -1 -o $(PAR_OUT_FILE) $(PAR_TIME) -v $(RUNJAVA) $$s $(MAIN_CLASS_NAME) $(APP_OPTIONS) $(PAR_RUN_OPTIONS) < /dev/null; cat $(PAR_OUT_FILE).* > $(PAR_OUT_FILE); rm -f $(PAR_OUT_FILE).*) 
