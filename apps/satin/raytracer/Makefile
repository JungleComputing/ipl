APP_OPTIONS = pics/balls2_hires.nff
SMALL_APP_OPTIONS = pics/balls2.nff
TEST_APP_OPTIONS = pics/balls2_small.nff

OUT= raytracer

MAIN=Raytracer.java

include ../../../config.mk
include ../config.mk

satin: 
	$(IBISC_COMMAND) -no-aborts $(MAIN)

clean:
	rm -f $(TRASH_FILES) test_out test_err test_res test_par_out test_par_err test_par_res test_res_out.ppm

include ../run.mk
