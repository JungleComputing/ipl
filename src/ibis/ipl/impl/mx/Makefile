CFLAGS = -O3 -Wall -fPIC -I/usr/local/Cluster-Apps/mx/mx-current/include
#/usr/local/Cluster-Apps/mx/mx-current/include
#../include

CC = gcc

LDFLAGS = -l myriexpress -L /usr/local/Cluster-Apps/mx/mx-current/lib

all: libjavamx.so

libjavamx.so: javamx.o linkmanager.o handlemanager.o
	$(CC) -shared -fPIC -Wl,-soname,libjavamx.so -o "libjavamx.so" $(LDFLAGS) javamx.o linkmanager.o handlemanager.o
	mv libjavamx.so ../../../../../lib/.
	
clean:
	rm -f *.o *core *~ *.so
