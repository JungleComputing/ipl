/* $Id$ */

/*
 * Created on 14.03.2005
 */
package ibis.mpj;

/**
 * Implementation of the collective operation: scatter.
 */
public class ColScatter {


    private Object sendbuf = null;
    private int sendoffset = 0;
    private int sendcount = 0;
    private Datatype sendtype = null;
    private Object recvbuf = null;
    private int recvoffset = 0;
    private int recvcount = 0;
    private Datatype recvtype = null;
    private int root = 0;
    private Intracomm comm = null;
    private int tag = 0;

    public ColScatter(Object sendbuf, int sendoffset, int sendcount, Datatype sendtype,
            Object recvbuf, int recvoffset, int recvcount, Datatype recvtype,
            int root, Intracomm comm, int tag) throws MPJException {
        this.sendbuf = sendbuf;
        this.sendoffset = sendoffset;
        this.sendcount = sendcount;
        this.sendtype = sendtype;
        this.recvbuf = recvbuf;
        this.recvoffset = recvoffset;
        this.recvcount = recvcount;
        this.recvtype = recvtype;
        this.root = root;
        this.comm = comm;
        this.tag = tag;

    }

    // flat tree algorithm:

    protected void call() throws MPJException {
        int rank = this.comm.rank();
        int size = this.comm.size();

        if (root < 0 || root >= size) {
            throw new MPJException("root rank " + root + " is invalid.");
        }

        if (rank == root) {


            for (int i = 0; i < size; i++) {


                if (i != rank)
                    this.comm.send(sendbuf, this.sendoffset + i * sendcount * sendtype.extent(), sendcount, sendtype, i, this.tag);

            }

            this.comm.localcopy2types(sendbuf, this.sendoffset + rank * sendcount * sendtype.extent(), sendcount, sendtype,
                    recvbuf, recvoffset, recvcount, recvtype); 
            //this.comm.recv(recvbuf, recvoffset, recvcount, recvtype, root, this.tag);
        }
        else {
            this.comm.recv(recvbuf, recvoffset, recvcount, recvtype, root, this.tag);
        }


    }


    /* alternative algorithm: 1. bcast and 2. filter */

    /*
       protected void execute() throws MPJException {

       int rank = this.comm.rank();
       int size = this.comm.size();
       ColBcast bcast = null;

       if (rank == root) {
       bcast = new ColBcast(sendbuf, sendoffset, size*sendcount, sendtype, root, this.comm, this.tag);
       bcast.execute();



       this.comm.localcopy(sendbuf, sendoffset + (rank * sendcount * (sendtype.Extent() + 1)), sendcount, sendtype,
       recvbuf, recvoffset, recvcount, recvtype);
    //System.arraycopy(sendbuf, sendoffset + (rank * sendcount) , recvbuf, recvoffset, recvcount);
    return;
       }

       Object tmpBuf = null;	
       int tmpLength = size * recvcount * (recvtype.Extent() + 1); 
       int recvLength = 0;

       if (recvbuf instanceof byte[]) {
       tmpBuf = new byte[tmpLength];
       recvLength = ((byte[])recvbuf).length;
       }
       else if (recvbuf instanceof char[]) {
       tmpBuf = new char[tmpLength];
       recvLength = ((char[])recvbuf).length;
       }
       else if (recvbuf instanceof short[]) {
       tmpBuf = new short[tmpLength];
       recvLength = ((short[])recvbuf).length;
       }
       else if (recvbuf instanceof boolean[]) {
       tmpBuf = new boolean[tmpLength];
       recvLength = ((boolean[])recvbuf).length;
       }
       else if (recvbuf instanceof int[])  {
       tmpBuf = new int[tmpLength];
       recvLength = ((int[])recvbuf).length;
       }
       else if (recvbuf instanceof long[]) {
       tmpBuf = new long[tmpLength];
       recvLength = ((long[])recvbuf).length;
       }
       else if (recvbuf instanceof float[]) {
       tmpBuf = new float[tmpLength];
       recvLength = ((float[])recvbuf).length;
       }
       else if (recvbuf instanceof double[]) {
       tmpBuf = new double[tmpLength];
       recvLength = ((double[])recvbuf).length;
       }
       else {
       tmpBuf = new Object[tmpLength];
       recvLength = ((Object[])recvbuf).length;
       }


       bcast = new ColBcast(tmpBuf, 0, size * recvcount, recvtype, root, this.comm, this.tag);
       bcast.execute();

       int tmpOffset = rank * recvcount  * (recvtype.Extent() + 1);
       int elementsToCopy = recvcount * (recvtype.Extent() + 1);

       if (recvLength < (recvoffset + elementsToCopy))
       {
       elementsToCopy = recvLength - recvoffset;
       }

    System.arraycopy(tmpBuf, tmpOffset, recvbuf, recvoffset, elementsToCopy);


       }
*/
}
