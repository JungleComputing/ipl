/* $Id$ */

import ibis.mpj.*;

public class Latency {

    static final int SEND_COUNT = 1000;
    static final int TRIES = 100;

        static public void main (String[] args) throws MPJException {
                MPJ.init(args);
                int size = MPJ.COMM_WORLD.size();
                int rank = MPJ.COMM_WORLD.rank();
                byte[] data = new byte[1];

                for(int j=0; j<TRIES; j++) {
                    long start = System.currentTimeMillis();
                    for(int i=0; i<SEND_COUNT; i++) {
                        if(rank==0) {
                            MPJ.COMM_WORLD.send(data, 0, 1, MPJ.BYTE, 1, i);
                            MPJ.COMM_WORLD.recv(data , 0, 1, MPJ.BYTE, 1, i);
                        } else {
                            MPJ.COMM_WORLD.recv(data , 0, 1, MPJ.BYTE, 0, i);
                            MPJ.COMM_WORLD.send(data, 0, 1, MPJ.BYTE, 0, i);
                        }
                    }

                    long end = System.currentTimeMillis();

                    double time = (((double) (end - start)) * 1000.0) / SEND_COUNT;

                    if(rank == 0) {
                        System.err.println("round-trip latency = " + time + " us");
                    }

                }

                MPJ.finish();
        }
}
