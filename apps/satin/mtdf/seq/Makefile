MANTAC  	= mantac

MANTA_FLAGS     = 

OBJ             = AwariBoard.o
OBJ            += Input.o
OBJ            += Mtdf.o
OBJ            += NodeType.o
OBJ            += TranspositionTable.o
OBJ            += TranspositionTableEntry.o

MAIN		=  Main.java

EXEC		= awari

.java.o:
	$(MANTAC) $(MANTA_FLAGS) -c $<

.SUFFIXES: .java

java:
	jikes *.java

manta: $(OBJ)
	$(MANTAC)    $(MANTA_FLAGS) $(MAIN) -o $(EXEC) *.o

clean:
	rm -rf *.[oasdhc] *~ *core *.jac *.gprof *out $(EXEC) *.class *.link LASM_CACHE

distclean: clean
	rm -rf output-*
