libNative.so: timestamp.o
	${LD} -shared -o libNative.so timestamp.o

timestamp.o: timestamp.c
	gcc -g -O2 -ansi -fPIC -Wall -c -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux timestamp.c

clean:
	rm timestamp.o
