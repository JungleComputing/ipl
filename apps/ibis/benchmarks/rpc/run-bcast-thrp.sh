#!/bin/bash

msgs=2000
size=100000
# size=0
timeout="-t 45:0"
# options=-bcast-all
options=-bcast
options="$options -one-way"

for (( i = 0; i < 5; i++ )) ; do

    if false ; then
	for n in 32 16 8 ; do
	    for s in panda ; do
		echo nonsequenced ser $s/inline
		prun $timeout -v -1 -no-panda  ~/ibis-fab/bin/run-das $n -Dibis.serialization=data \
		    -Dibis.name=$s -Dibis.mp.broadcast.native=off \
		    RPC $msgs $size $options
	    done
	done
    fi

    if false; then
	for n in 32 16 8 ; do
	    for s in panda ; do
		echo sequenced ser $s
		prun $timeout -v -1 -no-panda  ~/ibis-fab/bin/run-das $n -Dibis.serialization=data \
		    -Dibis.name=$s RPC $msgs $size $options -sequenced
	    done
	done
    fi

    if false ; then
	for n in 32 16 8 ; do
	    for s in net.bytes.gen.tcp_blk net.gm tcp panda ; do
		echo nonsequenced ser $s
		prun $timeout -v -1 -no-panda ~/ibis-fab/bin/run-das $n -Dibis.serialization=data \
		    -Dibis.name=$s RPC $msgs $size $options
	    done
	done
    fi

    if true ; then
	for n in 33 17 9 ; do
	    # for s in net.bytes.gen.tcp_blk net.gm tcp ; do
	    for s in net.gm net.bytes.gen.tcp_blk ; do
		echo sequenced ser $s
		prun $timeout -v -1 -no-panda ~/ibis-fab/bin/run-das $n -ns-d -Dibis.serialization=data \
		    -Dibis.name=$s RPC $msgs $size $options -sequenced
	    done
	done
    fi

done
