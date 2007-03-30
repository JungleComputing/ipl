/* $Id$ */

/*
 * Created on 18.02.2005
 */
package ibis.mpj;


/**
 * Implementation of the collective operation: alltoallv.
 */
public class ColAllToAllV {

    private Object sendbuf = null;
    private int sendoffset = 0;
    private int[] sendcount = null;
    private int[] sdispls = null;
    private Datatype sendtype = null;
    private Object recvbuf = null;
    private int recvoffset = 0;
    private int[] recvcount = null;
    private int[] rdispls = null;
    private Datatype recvtype = null;
    private Intracomm comm = null;
    private int tag = 0;

    public ColAllToAllV(Object sendbuf, int sendoffset, int[] sendcount, int[] sdispls,
            Datatype sendtype,
            Object recvbuf, int recvoffset, int[] recvcount, int[] rdispls,
            Datatype recvtype, Intracomm comm, int tag) {

        this.sendbuf = sendbuf;
        this.sendoffset = sendoffset;
        this.sendcount = sendcount;
        this.sdispls = sdispls;
        this.sendtype = sendtype;
        this.recvbuf = recvbuf;
        this.recvoffset = recvoffset;
        this.recvcount = recvcount;
        this.rdispls = rdispls;
        this.recvtype = recvtype;	
        this.comm = comm;
        this.tag = tag;
    }

    protected void call() throws MPJException {
        int size = this.comm.size();
        int rank = this.comm.rank();


        //		Status[] status = new Status[2 * size];
        Request[] request = new Request[2 * size];

        int reqCount = 0;

        for (int i = 0; i < this.comm.size(); i++) {
            int source = (rank + i) % size;

            if (recvcount[source] != 0) {
                request[reqCount] = this.comm.irecv(this.recvbuf, this.recvoffset + this.rdispls[source] * this.recvtype.extent(), 
                        this.recvcount[source], this.recvtype, source, this.tag);

                reqCount++;
            }
        }

        for (int i = 0; i < size; i++) {
            int dest = (rank + i) % size;

            if (sendcount[dest] != 0) {
                request[reqCount] = this.comm.isend(this.sendbuf, this.sendoffset + this.sdispls[dest] * this.sendtype.extent(),
                        this.sendcount[dest], this.sendtype, dest, this.tag);
                reqCount++;
            }
        }

        /* status = */ Request.waitAll(request);

    }
}
