SRC=  

MAIN = Latency.java
OUT=latency

include ../../../../config.mk

java: $(CLASS_FILES) $(MAIN_CLASS)

manta: $(MAIN) $(O_FILES)
	$(MANTAC_COMMAND) -o $(OUT) $(MAIN) $(O_FILES) ../../../../ibis/$(IBIS_LIB)

clean:
	rm -f $(TRASH_FILES)
