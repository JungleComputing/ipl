include ../config.mk

java: 
	cd ibis; $(MAKE) $(MFLAGS) java
	cd satin; $(MAKE) $(MFLAGS) java
	cd group; $(MAKE) $(MFLAGS) java

manta:
	cd ibis; $(MAKE) $(MFLAGS) manta
	cd satin; $(MAKE) $(MFLAGS) manta
	cd group; $(MAKE) $(MFLAGS) manta

clean:
	rm -f $(TRASH_FILES)
	cd ibis; $(MAKE) $(MFLAGS) clean
	cd rmi; $(MAKE) $(MFLAGS) clean
	cd satin; $(MAKE) $(MFLAGS) clean
	cd group; $(MAKE) $(MFLAGS) clean
