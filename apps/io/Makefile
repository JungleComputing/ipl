include ../../config.mk

java:
	cd new && $(MAKE) $(MFLAGS) java
	cd nocom && $(MAKE) $(MFLAGS) java
	cd sockets_MantaStream && $(MAKE) $(MFLAGS) java
	cd sockets_MantaTypedStream && $(MAKE) $(MFLAGS) java
	cd sockets_ObjectStream && $(MAKE) $(MFLAGS) java

manta:
	cd new && $(MAKE) $(MFLAGS) manta
	cd nocom && $(MAKE) $(MFLAGS) manta
	cd sockets_MantaStream && $(MAKE) $(MFLAGS) manta
	cd sockets_MantaTypedStream && $(MAKE) $(MFLAGS) java
	cd sockets_ObjectStream && $(MAKE) $(MFLAGS) manta

clean:
	rm -f $(TRASH_FILES)
	cd new && $(MAKE) $(MFLAGS) clean
	cd nocom && $(MAKE) $(MFLAGS) clean
	cd sockets_MantaStream && $(MAKE) $(MFLAGS) clean
	cd sockets_MantaTypedStream && $(MAKE) $(MFLAGS) java
	cd sockets_ObjectStream && $(MAKE) $(MFLAGS) clean
