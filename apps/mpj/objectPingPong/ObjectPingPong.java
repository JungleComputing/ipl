/* $Id$ */

import ibis.mpj.*;

public class ObjectPingPong {

    static final int SEND_COUNT = 1000;
    static final int TREE_NODES = 1023;
    static final int TRIES = 100;

        static public void main (String[] args) throws MPJException {
                MPJ.init(args);
                int size = MPJ.COMM_WORLD.size();
                int rank = MPJ.COMM_WORLD.rank();
                Object[] data = new Object[1];
                Tree t = new Tree(TREE_NODES);
                data[0] = t;

                for(int j=0; j<TRIES; j++) {
//                  MPJ.COMM_WORLD.barrier();

                    long start = System.currentTimeMillis();
                    for(int i=0; i<SEND_COUNT; i++) {
                        if(rank==0) {
                            MPJ.COMM_WORLD.send(data, 0, 1, MPJ.OBJECT, 1, i);
                            MPJ.COMM_WORLD.recv(data , 0, 1, MPJ.OBJECT, 1, i);
                        } else {
                            MPJ.COMM_WORLD.recv(data , 0, 1, MPJ.OBJECT, 0, i);
                            MPJ.COMM_WORLD.send(data, 0, 1, MPJ.OBJECT, 0, i);
                        }
                    }
//                  MPJ.COMM_WORLD.barrier();

                    long end = System.currentTimeMillis();

                    double time = (((double) (end - start)) * 1000.0) / SEND_COUNT;

                    double speed = (time * 1000.0) / (double) SEND_COUNT;
                    double tp = ((SEND_COUNT * TREE_NODES * Tree.PAYLOAD) / (1024*1024)) / (time / 1000.0);

                    if(rank == 0) {
                        System.err.println(SEND_COUNT + " calls took "
                                           + (time / 1000.0) + " s, time/call = " + speed
                                           + " us, throughput = " + tp + " MB/s, msg size = " + (TREE_NODES * Tree.PAYLOAD));
                    }
                }

                MPJ.finish();
        }
}
