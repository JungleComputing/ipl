include ../../../config.mk

java: 
	cd latency; $(MAKE) $(MFLAGS) java
	cd throughput; $(MAKE) $(MFLAGS) java

manta:
	cd latency; $(MAKE) $(MFLAGS) manta
	cd throughput; $(MAKE) $(MFLAGS) manta

clean:
	rm -f $(TRASH_FILES)
	cd rpc; $(MAKE) $(MFLAGS) clean
	cd latency; $(MAKE) $(MFLAGS) clean
	cd throughput; $(MAKE) $(MFLAGS) clean
