include ../config.mk

java: 
	cd ibis && $(MAKE) $(MFLAGS) java
	cd satin && $(MAKE) $(MFLAGS) java
	-cd gmi && $(MAKE) $(MFLAGS) java

manta:
	cd ibis && $(MAKE) $(MFLAGS) manta
	cd satin && $(MAKE) $(MFLAGS) manta
	-cd gmi && $(MAKE) $(MFLAGS) manta

clean:
	rm -f $(TRASH_FILES)
	-cd ibis && $(MAKE) $(MFLAGS) clean
	-cd rmi && $(MAKE) $(MFLAGS) clean
	-cd satin && $(MAKE) $(MFLAGS) clean
	-cd gmi && $(MAKE) $(MFLAGS) clean
	-cd extra && $(MAKE) $(MFLAGS) clean
	-cd io && $(MAKE) $(MFLAGS) clean
	-cd repmi && $(MAKE) $(MFLAGS) clean
