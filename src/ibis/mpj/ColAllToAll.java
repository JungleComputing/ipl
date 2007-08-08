/* $Id$ */

/*
 * Created on 05.04.2005
 */
package ibis.mpj;

import org.apache.log4j.Logger;

/**
 * Implementation of the collective operation: alltoall.
 */
public class ColAllToAll {
    static Logger logger = Logger.getLogger(ColAllToAll.class.getName());

    private Object sendbuf = null;
    private int sendoffset = 0;
    private int sendcount = 0;
    private Datatype sendtype = null;
    private Object recvbuf = null;
    private int recvoffset = 0;
    private int recvcount = 0;
    private Datatype recvtype = null;
    private Intracomm comm = null;
    private int tag = 0;

    public ColAllToAll(Object sendbuf, int sendoffset, int sendcount, Datatype sendtype,
            Object recvbuf, int recvoffset, int recvcount, Datatype recvtype, 
            Intracomm comm, int tag) {

        this.sendbuf = sendbuf;
        this.sendoffset = sendoffset;
        this.sendcount = sendcount;
        this.sendtype = sendtype;
        this.recvbuf = recvbuf;
        this.recvoffset = recvoffset;
        this.recvcount = recvcount;
        this.recvtype = recvtype;	
        this.comm = comm;
        this.tag = tag;
    }

    protected void call() throws MPJException {
        int size = this.comm.size();
        int rank = this.comm.rank();


        Status[] status = new Status[2 * size];
        Request[] request = new Request[2 * size];

        int reqCount = 0;

        for (int i = 0; i < this.comm.size(); i++) {
            int source = (rank + i) % size;

            request[reqCount] = this.comm.irecv(this.recvbuf, this.recvoffset + (source * recvcount * recvtype.extent()), 
                    this.recvcount, this.recvtype, source, this.tag);

            reqCount++;
        }

        for (int i = 0; i < size; i++) {
            int dest = (rank + i) % size;

            request[reqCount] = this.comm.isend(this.sendbuf, this.sendoffset + (dest * sendcount * sendtype.extent()),
                    this.sendcount, this.sendtype, dest, this.tag);
            reqCount++;

        }

        status = Request.waitAll(request);

        if (logger.isDebugEnabled()) {
            for (int i = 0; i < size; i++) {
                logger.debug("status-counts: " + i + ": " + status[i].getCount(recvtype));
            }
        }
    }
}
