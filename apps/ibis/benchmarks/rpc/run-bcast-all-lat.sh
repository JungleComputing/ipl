#!/bin/bash

msgs=2000
# size=100000
size=0
options=-bcast-all
# options=-bcast
# options="$options -one-way"

for (( i = 0; i < 5; i++ )) ; do

    for n in 32 16 8 ; do
	for s in panda ; do
	    echo nonsequenced ser $s/inline
	    prun -v -1 -no-panda  ~/ibis-fab/bin/run-das $n -Dibis.serialization=data \
	    	-Dibis.name=$s -Dibis.mp.broadcast.native=off \
		RPC $msgs $size $options
	done
    done

    for n in 32 16 8 ; do
	for s in panda ; do
	    echo sequenced ser $s
	    prun -v -1 -no-panda  ~/ibis-fab/bin/run-das $n -Dibis.serialization=data \
	    	-Dibis.name=$s RPC $msgs $size $options -sequenced
	done
    done

    for n in 32 16 8 ; do
    	for s in tcp net.bytes.gen.tcp_blk panda net.gm ; do
	    echo nonsequenced ser $s
	    prun -v -1 -no-panda -t 5:0  ~/ibis-fab/bin/run-das $n -Dibis.serialization=data \
	    	-Dibis.name=$s RPC $msgs $size $options
	done
    done

    for n in 33 17 9 ; do
    	for s in tcp net.bytes.gen.tcp_blk net.gm ; do
	    echo sequenced ser $s
	    prun -v -1 -no-panda -t 10:0  ~/ibis-fab/bin/run-das $n -ns-d -Dibis.serialization=data \
	    	-Dibis.name=$s RPC $msgs $size $options -sequenced
	done
    done

done
