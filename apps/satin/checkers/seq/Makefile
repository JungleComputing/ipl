JAVA_SRC += Position.java
JAVA_SRC += OrcaRandom.java
MAIN_SRC += Checkers.java

O_FILES = $(JAVA_SRC:%.java=%.o)
CLASS_FILES  = $(JAVA_SRC:%.java=%.class)
CLASS_FILES += $(MAIN_SRC:%.java=%.class)

MANTAC_COMMAND = /usr/local/VU/manta/mantac
JAVAC_COMMAND = jikes
OUT = checkers

java: $(CLASS_FILES)

clean:
	rm -rf *.class *~ JobQueue_* SharedInt_* *.c *.h *.jac *.o *core *.s $(OUT) LASM_CACHE

.SUFFIXES:
.SUFFIXES:      .o .java .class

.java.o:
	$(MANTAC_COMMAND) -c $*.java

.java.class:
	$(JAVAC_COMMAND) $*.java
