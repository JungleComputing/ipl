/* $Id$ */

/*
 * Created on 18.02.2005
 */
package ibis.mpj;


/**
 * Implementation of the collective operation: reduce
 */
public class ColReduce {
    private final boolean DEBUG = false;

    private Object sendbuf = null;
    private int sendOffset = 0;
    private Object recvbuf = null;
    private int recvOffset = 0;
    private int count = 0;
    private Datatype datatype = null;
    private Intracomm comm = null;
    private Op op = null;
    private int root = 0;
    private int tag = 0;


    public ColReduce(Object sendBuf, int sendOffset, Object recvBuf, int recvOffset,  int count, Datatype datatype, Op op, int root, Intracomm comm, int tag) {
        this.sendbuf = sendBuf;
        this.sendOffset = sendOffset;
        this.recvbuf = recvBuf;
        this.recvOffset = recvOffset;
        this.count = count;
        this.datatype = datatype;
        this.comm = comm;
        this.op = op;
        this.root = root;
        this.tag = tag;

    }


    protected void call() throws MPJException {
        if (root < 0 || root >= this.comm.size()) {
            throw new MPJException("root rank " + root + " is invalid.");
        }

        if (this.op.commute) {
            executeBin();
        }
        else {
            executeFlat();
        }
    }

    // binomial tree algorithm (taken from CCJ)
    private void executeBin() throws MPJException {
        int mask, relrank, peer;
        int rank = this.comm.rank();
        int size = this.comm.size();


        if (DEBUG) {
            System.out.println(rank + ": Reduce started. Root = " + root + " groupsize = " + size + " counter<< = " + this.tag);
        }

        Object recv = null;
        if (sendbuf instanceof byte[]) {
            recv = new byte[((byte[])this.recvbuf).length];
        }
        else if (sendbuf instanceof char[]) {
            recv = new char[((char[])this.recvbuf).length];		
        }
        else if (sendbuf instanceof short[]) {
            recv = new short[((short[])this.recvbuf).length];		
        }
        else if (sendbuf instanceof boolean[]) {
            recv = new boolean[((boolean[])this.recvbuf).length];		
        }
        else if (sendbuf instanceof int[])  {
            recv = new int[((int[])this.recvbuf).length];		
        }
        else if (sendbuf instanceof long[]) {
            recv = new long[((long[])this.recvbuf).length];		
        }
        else if (sendbuf instanceof float[]) {
            recv = new float[((float[])this.recvbuf).length];		
        }
        else if (sendbuf instanceof double[]) {
            recv = new double[((double[])this.recvbuf).length];		
        }
        else {
            recv = new Object[((Object[])this.recvbuf).length];		
        }
        this.comm.localcopy1type(this.sendbuf, this.sendOffset, this.recvbuf, this.recvOffset, this.count, this.datatype);


        mask = 1;

        relrank = (rank - root + size) % size;


        while (mask < size) {
            if ((mask & relrank) == 0) { // receive and reduce 
                peer = (relrank | mask);


                if (peer < size) {
                    peer = (peer + root) % size;

                    if (DEBUG) {
                        System.out.println(rank + ": Reduce receive from "+ peer);
                        System.out.flush();
                    }


                    this.comm.recv(recv, this.recvOffset, this.count, this.datatype, peer, this.tag);

                    op.call(recv, recvOffset, recvbuf, recvOffset, count, datatype);
                }
            } else { // send and terminate 
                peer = ((relrank & (~mask)) + root) % size;

                if (DEBUG) {
                    System.out.println(rank + ": Reduce send to "+ peer);
                    System.out.flush();
                }

                this.comm.send(this.recvbuf, this.recvOffset, this.count, this.datatype, peer, this.tag);
                break;
            }

            mask <<= 1;
        }

        if (DEBUG) {
            System.out.println(rank + ": reduce done.");
        }

    }


    private void executeFlat() throws MPJException {
        if (this.comm.rank() == root) {
            Object recv = null;
            if (sendbuf instanceof byte[]) {
                recv = new byte[((byte[])this.recvbuf).length];
                System.arraycopy(this.sendbuf, this.sendOffset, this.recvbuf, this.recvOffset, this.count);
            }
            else if (sendbuf instanceof char[]) {
                recv = new char[((char[])this.recvbuf).length];		
                System.arraycopy(this.sendbuf, this.sendOffset, this.recvbuf, this.recvOffset, this.count);
            }
            else if (sendbuf instanceof short[]) {
                recv = new short[((short[])this.recvbuf).length];		
                System.arraycopy(this.sendbuf, this.sendOffset, this.recvbuf, this.recvOffset, this.count);
            }
            else if (sendbuf instanceof boolean[]) {
                recv = new boolean[((boolean[])this.recvbuf).length];		
                System.arraycopy(this.sendbuf, this.sendOffset, this.recvbuf, this.recvOffset, this.count);
            }
            else if (sendbuf instanceof int[])  {
                recv = new int[((int[])this.recvbuf).length];		
                System.arraycopy(this.sendbuf, this.sendOffset, this.recvbuf, this.recvOffset, this.count);
            }
            else if (sendbuf instanceof long[]) {
                recv = new long[((long[])this.recvbuf).length];		
                System.arraycopy(this.sendbuf, this.sendOffset, this.recvbuf, this.recvOffset, this.count);
            }
            else if (sendbuf instanceof float[]) {
                recv = new float[((float[])this.recvbuf).length];		
                System.arraycopy(this.sendbuf, this.sendOffset, this.recvbuf, this.recvOffset, this.count);
            }
            else if (sendbuf instanceof double[]) {
                recv = new double[((double[])this.recvbuf).length];		
                System.arraycopy(this.sendbuf, this.sendOffset, this.recvbuf, this.recvOffset, this.count);
            }
            else {

                recv = new Object[((Object[])this.recvbuf).length];		

                Object[] obj = (Object[])sendbuf;

                for (int j = 0; j < this.count; j++) {
                    ((Object[])recvbuf)[j+this.recvOffset] = obj[j+this.sendOffset];

                }
            }

            for (int i=0; i < this.comm.size(); i++)
            {
                if (i != root) {
                    this.comm.recv(recv, recvOffset, count, datatype, i, this.tag);

                    op.call(recv, recvOffset, recvbuf, recvOffset, count, datatype);

                }
            }
        }
        else {
            this.comm.send(sendbuf, sendOffset, count, datatype, root, this.tag);
        }
    }
}
