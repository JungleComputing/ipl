SRC=  Repository.java 
SRC+= RepositoryObserver.java 
MAIN = 
OUT=

PACKAGE	= org.apache.bcel

include ../../../../config.mk

java:	java_local
	(cd verifier/structurals && make java)
	(cd verifier/statics && make java)
	(cd classfile && make java)
	(cd util && make java)

java_local:	$(CLASS_FILES) $(MAIN_CLASS)

manta:

clean:
	rm -f $(TRASH_FILES) *.so *.a
	(cd verifier/structurals && make clean)
	(cd verifier/statics && make clean)
	(cd classfile && make clean)
	(cd util && make clean)

distclean:	clean
