include ../../config.mk

java: 
	cd latency && $(MAKE) $(MFLAGS) java
	cd throughput && $(MAKE) $(MFLAGS) java

manta:
	cd latency && $(MAKE) $(MFLAGS) manta
	cd throughput && $(MAKE) $(MFLAGS) manta

clean:
	rm -f $(TRASH_FILES)
	cd latency && $(MAKE) $(MFLAGS) clean
	cd throughput && $(MAKE) $(MFLAGS) clean
	cd asp_toplass && $(MAKE) $(MFLAGS) clean
	cd rmi-bench && $(MAKE) $(MFLAGS) clean
	cd sor_toplass && $(MAKE) $(MFLAGS) clean
	cd barnes && $(MAKE) $(MFLAGS) clean
	cd water && $(MAKE) $(MFLAGS) clean
	cd tsp && $(MAKE) $(MFLAGS) clean
	cd fft && $(MAKE) $(MFLAGS) clean
	cd radix && $(MAKE) $(MFLAGS) clean
	cd leq && $(MAKE) $(MFLAGS) clean
