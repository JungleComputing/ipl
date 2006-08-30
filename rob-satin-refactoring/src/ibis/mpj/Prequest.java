/* $Id$ */

/*
 * Created on 07.02.2005
 */
package ibis.mpj;

/**
 * Request class for persistent point-to-point communication.
 */
public class Prequest extends Request{
    protected Object buf = null;
    protected int offset = 0;
    protected int count = 0;
    protected Datatype datatype = null;
    protected int rank = 0;
    protected int tag = 0;
    protected int mode = 0;
    protected Comm comm = null;


    protected static final int OP_SEND = 0;
    protected static final int OP_BSEND = 1;
    protected static final int OP_SSEND = 2;
    protected static final int OP_RSEND = 3;
    protected static final int OP_RECV = 4;

    /**
     * Activate a persistent communication request.
     * The communication is completed by using the requests in one of the operations
     * Request.Wait, Request.test, Request.waitAny, Request.testAny, Request.waitAll, Request.testAll,
     * Request.waitSome, Request.testSome. On succesful completion the request becomes inactive. It can
     * be reactivated by a further call to start.
     */
    public void start() {
        try {
            Request req;
            if(mode == OP_SEND) {
                req = comm.isend(this.buf, this.offset, this.count, this.datatype, this.rank, this.tag);
            }
            else if(mode == OP_BSEND) {
                req = comm.ibsend(this.buf, this.offset, this.count, this.datatype, this.rank, this.tag);
            }
            else if(mode == OP_SSEND) {
                req = comm.issend(this.buf, this.offset, this.count, this.datatype, this.rank, this.tag);
            }
            else if(mode == OP_RSEND) {
                req = comm.irsend(this.buf, this.offset, this.count, this.datatype, this.rank, this.tag);
            }
            else {// if(mode == OP_RECV) {
                req = comm.irecv(this.buf, this.offset, this.count, this.datatype, this.rank, this.tag);
            }

            this.ibisMPJComm = req.ibisMPJComm;
            this.ibisMPJCommThread = req.ibisMPJCommThread;
            }
            catch (MPJException e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
        }


        /**
         * Activate a list of communication requests.
         * @param arrayOfRequests array of persistent communication requests
         */
        public static void startAll(Prequest[] arrayOfRequests) {
            try {

                for (int i = 0; i < arrayOfRequests.length; i++) {
                    Request req;
                    if(arrayOfRequests[i].mode == OP_SEND) {
                        req = arrayOfRequests[i].comm.isend(arrayOfRequests[i].buf, 
                                arrayOfRequests[i].offset, 
                                arrayOfRequests[i].count, 
                                arrayOfRequests[i].datatype, 
                                arrayOfRequests[i].rank, 
                                arrayOfRequests[i].tag);
                    }
                    else if(arrayOfRequests[i].mode == OP_BSEND) {
                        req = arrayOfRequests[i].comm.ibsend(arrayOfRequests[i].buf, 
                                arrayOfRequests[i].offset, 
                                arrayOfRequests[i].count, 
                                arrayOfRequests[i].datatype, 
                                arrayOfRequests[i].rank, 
                                arrayOfRequests[i].tag);
                    }
                    else if(arrayOfRequests[i].mode == OP_SSEND) {
                        req = arrayOfRequests[i].comm.issend(arrayOfRequests[i].buf, 
                                arrayOfRequests[i].offset, 
                                arrayOfRequests[i].count, 
                                arrayOfRequests[i].datatype, 
                                arrayOfRequests[i].rank, 
                                arrayOfRequests[i].tag);
                    }
                    else if(arrayOfRequests[i].mode == OP_RSEND) {
                        req = arrayOfRequests[i].comm.irsend(arrayOfRequests[i].buf, 
                                arrayOfRequests[i].offset, 
                                arrayOfRequests[i].count, 
                                arrayOfRequests[i].datatype, 
                                arrayOfRequests[i].rank, 
                                arrayOfRequests[i].tag);
                    }
                    else { //if(arrayOfRequests[i].mode == OP_RECV) {
                        req = arrayOfRequests[i].comm.irecv(arrayOfRequests[i].buf, 
                                arrayOfRequests[i].offset, 
                                arrayOfRequests[i].count, 
                                arrayOfRequests[i].datatype, 
                                arrayOfRequests[i].rank, 
                                arrayOfRequests[i].tag);
                    }
                    arrayOfRequests[i].ibisMPJComm = req.ibisMPJComm;
                    arrayOfRequests[i].ibisMPJCommThread = req.ibisMPJCommThread;
                    }
                }

                catch (MPJException e) {
                    System.err.println(e.getMessage());
                    e.printStackTrace();
                }
            }
        }
