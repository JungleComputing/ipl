include ../../config.mk

java: 
	cd test && $(MAKE) $(MFLAGS) java

manta:
	cd test && $(MAKE) $(MFLAGS) manta

clean:
	rm -f $(TRASH_FILES)
	-cd test && $(MAKE) $(MFLAGS) clean
