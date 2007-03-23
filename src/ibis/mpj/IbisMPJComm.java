/* $Id$ */

/*
 * Created on 04.02.2005
 */
package ibis.mpj;


import ibis.ipl.*;



/**
 * Implementation of the basic point-to-point communication modes on top of ibis.
 */
public class IbisMPJComm extends Thread {
    private final boolean DEBUG = false;

    protected final static int OP_IRECV = 1;
    protected final static int OP_ISEND = 2;
    protected final static int OP_IBSEND = 3;
    protected final static int OP_ISSEND = 4;
    protected final static int OP_IRSEND = 5;
    protected final static int OP_IPROBE = 6;
    protected final static int OP_PROBE = 7;

    private boolean buffered = false;

    protected Request request;
    protected Object buf;
    protected int offset;
    protected int count;
    protected Datatype datatype;
    protected int dest;
    protected int source;
    protected int tag;
    protected int opCode;
    protected Comm comm;
    protected MPJObject m = null;
    protected int myRank;
    protected Status status = null;
    protected int mode = -1;
    protected int contextId = -1;

    protected boolean finished = false;

    public IbisMPJComm() {
        // does nothing by default
    }

    public IbisMPJComm(Comm comm, int myRank, Object buf, int offset, int count, 
            Datatype datatype, int source, int dest, int tag, int mode) {

        this.buf = buf;
        this.offset = offset;
        this.count = count;
        this.datatype = datatype;
        this.dest = dest;
        this.source = source;
        this.tag = tag;
        this.mode = mode;
        this.comm = comm;
        this.myRank = myRank;
        this.request = new Request();
        this.request.setIbisMPJComm(this);
        this.status = new Status();
        this.status.setSource(0);
        this.status.setTag(0);
        this.status.setCount(0);
        this.contextId = comm.contextId;
        this.finished = false;

    }

    protected Request getRequest() {
        return(request);
    }

    protected Status getStatus() {
        return(status);
    }


    private void doIRecvAnySource() throws MPJException {
        int typeSize = 1;
        int realCount = 0;
        int extent = datatype.extent();
        MPJObject obj = null;
        this.status = new Status();
        ReadMessage msg = null;
        // boolean DEBUG = true;



        if (DEBUG) {
            System.out.println("irecv: " + this.comm.rank() + ": " + MPJ.getMyId() + ": ANY_SOURCE");
        }

        int src = 0;
        boolean msgReceived = false;
        while (!msgReceived) {

            IbisIdentifier id = this.comm.group().getId(src);
            Connection con = MPJ.getConnection(id);
            MPJObjectQueue queue = con.getRecvQueue();


            if (queue.isLocked()) {
                src = nextSource(src);
                continue;
            }
            // 	first check the object queue and hold the monitor on it
            synchronized(queue) {

                // set Lock to inform another irecv using MPJ.ANY_SOURCE
                queue.lock();

                obj = null;
                obj = queue.getObject(contextId, tag);

                // if queue has not the requested object, try to connect to the receiveport
                if (obj == null) {


                    obj = new MPJObject();
                    msg = null;


                    msg = con.pollForMessage();

                    if (msg == null) {
                        src = nextSource(src);

                        queue.release();
                        if (DEBUG) System.out.println("message was null... go to next rank");
                        continue;
                    }
                    // 	get message header
                    con.receiveHeader(msg, obj.desc);

                    if(DEBUG) {
                        System.out.println("recv tag:       " + obj.getTag());
                        System.out.println("recv contextId: " + obj.getContextId());
                        System.out.println("recv type:      " + obj.getBaseDatatype());
                        System.out.println("recv count:     " + obj.getNumberOfElements());
                        System.out.println("recv offset:    " + offset);

                        System.out.println("supposed count: " + count * datatype.extent());
                    }


                    // 	the message was expected
                    if (((obj.getTag() == tag) || 
                                ((tag == MPJ.ANY_TAG) && (obj.getTag() >= 0))) && 
                            (obj.getContextId() == contextId)) {


                        if (!obj.isBuffered()) {
                            // 	if count is larger than the array received 
                            // and message NOT buffered -> cut count
                            if ((count*datatype.extent() > obj.getNumberOfElements()) && (!obj.isBuffered())) {
                                count = obj.getNumberOfElements();
                                con.receiveData(msg, buf, offset, count * datatype.extent());	
                            }
                            // 	if count is smaller than the array received
                            // 	and message NOT buffered -> copy buffer
                            else if(((count*datatype.extent()) < obj.getNumberOfElements()) && (!obj.isBuffered())) {

                                obj.initBuffer();
                                con.receiveData(msg, obj.buffer, 0, obj.getNumberOfElements());

                                castBuffer(obj, buf, offset, count*extent);

                            }

                            // count is equal to received array size 
                            // and message NOT buffered -> receive it directly
                            else if (!obj.isBuffered()){
                                con.receiveData(msg, buf, offset, count * datatype.extent());
                            }



                        }							
                        // else the message was buffered -> unbuffer it
                        else {
                            byte[] byteBuf = new byte[obj.getNumberOfElements()+1];

                            con.receiveData(msg, byteBuf, 0, obj.getNumberOfElements());

                            realCount = BufferOps.unBuffer(byteBuf, buf, offset, count * extent);
                        }

                        typeSize = datatype.byteSize;
                        realCount = count * datatype.extent();
                        msgReceived = true;						
                            }
                    //		the message was NOT expected -> move to queue
                    else {
                        if(DEBUG) {
                            System.out.println("Message was NOT expected -> move to queue");
                        }
                        obj.initBuffer();
                        con.receiveData(msg, obj.buffer, 0, obj.getNumberOfElements());

                        queue.addObject(obj);
                        msgReceived = false;

                        src = nextSource(src);

                    }

                }
                // found the object inside the queue
                else {
                    if (DEBUG) System.out.println("found the object inside the queue");

                    typeSize = datatype.getByteSize();
                    // 	message was NOT buffered -> normal copy
                    if (!obj.isBuffered()) {
                        realCount = castBuffer(obj, buf, offset, count*extent);
                    }

                    // 	message was buffered -> unbuffer it
                    else {
                        realCount = BufferOps.unBuffer(obj.getObjectData(), buf, offset, count * (extent));
                    }
                    msgReceived = true;
                }
                queue.release();
            }
        }



        status.setSource(src);
        status.setTag(obj.getTag());
        status.setCount(realCount);
        status.setSize(realCount * typeSize);
    }

    private int nextSource(int prevSrc) throws MPJException {
        /*int newSrc =*/ prevSrc++;

        if (prevSrc == this.comm.size()) {
            prevSrc = 0;
        }

        return (prevSrc);

    }


    private void doIRecvSource(boolean blocking) throws MPJException {
        int typeSize = 1;
        int realCount = 0;
        int extent = datatype.extent();
        MPJObject obj = null;
        this.status = new Status();
        ReadMessage msg = null;
        //boolean DEBUG = true;


        IbisIdentifier id = this.comm.group().getId(source);
        Connection con = MPJ.getConnection(id);
        MPJObjectQueue queue = con.getRecvQueue();

        if (DEBUG) {
            System.out.println("irecv: " + this.comm.rank() + ": " + MPJ.getMyId() + ": SOURCE: " + id + "; rank: " + source);
        }

        // 	first check the object queue and hold the monitor on it
        boolean msgReceived = false;
        while (!msgReceived) {

            synchronized(queue) {

                // set Lock to inform another irecv using MPJ.ANY_SOURCE
                queue.lock();

                obj = null;
                obj = queue.getObject(contextId, tag);
                // found the object inside the queue
                if ((obj != null)) {
                    if(DEBUG) {
                        System.out.println("Message was found inside the queue expected -> move out of queue (mytag: " + tag + ")");
                    }
                    typeSize = datatype.getByteSize();
                    // 	message was NOT buffered -> normal copy
                    if (!obj.isBuffered()) {
                        realCount = castBuffer(obj, buf, offset, count*extent);
                    }

                    // message was buffered -> unbuffer it
                    else {
                        realCount = BufferOps.unBuffer(obj.getObjectData(), buf, offset, count * (extent));
                    }
                    if(DEBUG) {
                        System.out.println("recv tag:       " + obj.getTag());
                        System.out.println("recv contextId: " + obj.getContextId());
                        System.out.println("recv type:      " + obj.getBaseDatatype());
                        System.out.println("recv count:     " + obj.getNumberOfElements());
                        System.out.println("recv offset:    " + offset);

                        System.out.println("supposed tag:   " + this.tag);
                        System.out.println("supposed contID:" + this.contextId);
                        System.out.println("supposed count: " + count * datatype.extent());
                    }
                    msgReceived = true;

                }		
                // if queue has not the requested object, try to connect to the receiveport
                else if (obj == null) {

                    obj = new MPJObject();
                    msg = null;

                    if(!blocking && queue.size() != 0) {
                        // We need to give other threads
                        // the chance to obtain the
                        // message. For instance a program
                        // could post an asynchronous receive,
                        // and then do a barrier (Ceriel).
                        msg =con.pollForMessage();

                        if (msg==null) {
                            queue.release();
                            continue;
                        }
                    }
                    else {
                        msg = con.getNextMessage();	
                    }

                    // 	get message header
                    con.receiveHeader(msg, obj.desc);

                    if(DEBUG) {
                        System.out.println("recv tag:       " + obj.getTag());
                        System.out.println("recv contextId: " + obj.getContextId());
                        System.out.println("recv type:      " + obj.getBaseDatatype());
                        System.out.println("recv count:     " + obj.getNumberOfElements());
                        System.out.println("recv offset:    " + offset);

                        System.out.println("supposed tag:   " + this.tag);
                        System.out.println("supposed contID:" + this.contextId);
                        System.out.println("supposed count: " + count * datatype.extent());
                        if (obj.isBuffered()) System.out.println("BUFFERED");
                        else System.out.println("NOT BUFFERED");
                    }


                    // 	the message was expected
                    if (((obj.getTag() == tag) || 
                                ((tag == MPJ.ANY_TAG) && (obj.getTag() >= 0))) && 
                            (obj.getContextId() == contextId)) {


                        if (!obj.isBuffered()) {
                            // if count is larger than the array received 
                            // and message NOT buffered -> cut count
                            if (((count*datatype.extent()) > obj.getNumberOfElements()) && (!obj.isBuffered())) {
                                count = obj.getNumberOfElements();
                                con.receiveData(msg, buf, offset, count * datatype.extent());	
                            }
                            // 	if count is smaller than the array received
                            // 	and message NOT buffered -> copy buffer
                            else if((count*(datatype.extent()+1) < obj.getNumberOfElements()) && (!obj.isBuffered())) {
                                obj.initBuffer();
                                con.receiveData(msg, obj.buffer, 0, obj.getNumberOfElements());

                                castBuffer(obj, buf, offset, count*extent);
                            }

                            // count is equal to received array size 
                            // and message NOT buffered -> receive it directly
                            else if (!obj.isBuffered()){
                                con.receiveData(msg, buf, offset, count * datatype.extent());
                            }
                        }							
                        // else the message was buffered -> unbuffer it
                        else {
                            if (DEBUG) {
                                System.out.println("Message was expected and buffered -> unbuffer it");

                            }
                            byte[] byteBuf = new byte[obj.getNumberOfElements()];

                            con.receiveData(msg, byteBuf, 0, obj.getNumberOfElements());
                            realCount = BufferOps.unBuffer(byteBuf, buf, offset, count * extent);
                        }

                        typeSize = datatype.byteSize;
                        realCount = count * datatype.extent();

                        msgReceived = true;						
                            }
                    // 	the message was NOT expected -> move to queue
                    else {
                        if(DEBUG) {
                            System.out.println("Message was NOT expected -> move to queue");
                        }
                        obj.initBuffer();
                        con.receiveData(msg, obj.buffer, 0, obj.getNumberOfElements());

                        queue.addObject(obj);
                        msgReceived = false;

                    }

                }

                queue.release();

            }

        }			

        status.setSource(source);
        status.setTag(obj.getTag());
        status.setCount(realCount);
        status.setSize(realCount * typeSize);

    }



    protected void doIrecv(boolean blocking) throws MPJException {
        if (source == MPJ.PROC_NULL) {
            return;
        }

        if (source == MPJ.ANY_SOURCE) {
            doIRecvAnySource();
        }
        else {
            doIRecvSource(blocking);
        }

    }


    private int castBuffer(Object inbuf, Object outbuf, int offset, int count) {
        if ((buf instanceof byte[]) && (datatype != MPJ.OBJECT)) {
            return(((MPJObject)inbuf).cast2ByteArray(buf, offset, count));
        }
        else if (buf instanceof char[]) {
            return(((MPJObject)inbuf).cast2CharArray(buf, offset, count));
        }
        else if (buf instanceof short[]) {
            return(((MPJObject)inbuf).cast2ShortArray(buf, offset, count));
        }
        else if (buf instanceof boolean[]) {
            return(((MPJObject)inbuf).cast2BooleanArray(buf, offset, count));
        }
        else if (buf instanceof int[])  {
            return(((MPJObject)inbuf).cast2IntArray(buf, offset, count));
        }
        else if (buf instanceof long[]) {
            return(((MPJObject)inbuf).cast2LongArray(buf, offset, count));
        }
        else if (buf instanceof float[]) {
            return(((MPJObject)inbuf).cast2FloatArray(buf, offset, count));
        }
        else if (buf instanceof double[]) {
            return(((MPJObject)inbuf).cast2DoubleArray(buf, offset, count));
        }
        else {
            return(((MPJObject)inbuf).cast2ObjectArray(buf, offset, count));
        }
    }






    protected void doIsend() throws MPJException{

        if (dest == MPJ.PROC_NULL) return;

        //	boolean DEBUG = true;

        MPJObject sendObj = new MPJObject(tag, this.contextId, false, datatype.type, count * datatype.extent());
        sendObj.setBuffered(this.buffered);





        if(DEBUG) {
            System.out.println("send tag:       " + sendObj.getTag());
            System.out.println("send contextId: " + sendObj.getContextId());
            System.out.println("send type:      " + sendObj.getBaseDatatype());
            System.out.println("send count:     " + sendObj.getNumberOfElements());
            System.out.println("send offset:    " + offset);
        }

        try {
            IbisIdentifier id = this.comm.group().getId(dest);
            Connection con = MPJ.getConnection(id);
            if (DEBUG) {
                System.out.println("isend: " + this.comm.rank() + ": " + MPJ.getMyId() + ": DEST: " + id + "; rank: " + dest + "; tag: " + tag);
            }


            con.putMPJObject(sendObj, buf, offset, count * datatype.extent());
            con.sendMPJObject();



        }
        catch (MPJException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }

    }






    protected void doIbsend() throws MPJException {
        if (dest == MPJ.PROC_NULL) {
            return;
        }


        synchronized (MPJ.bsendCount) {
            MPJ.bsendCount.bsends++;
            MPJ.bsendCount.notifyAll();
        }

        byte[] sendBuf = MPJ.getAttachedBuffer();
        int realcount = 0;
        if (sendBuf == null) throw new MPJException("No user space buffer attached to MPJ.");

        if (datatype.type == MPJ.BYTE.type) {
            realcount = BufferOps.bufferByte(buf, sendBuf, this.offset, this.count * this.datatype.extent(), 0);
        }
        else if (datatype.type == MPJ.CHAR.type) {
            realcount = BufferOps.bufferChar(buf, sendBuf, this.offset, this.count * this.datatype.extent(), 0);
        }
        else if (datatype.type == MPJ.SHORT.type) {
            realcount = BufferOps.bufferShort(buf, sendBuf, this.offset, this.count * this.datatype.extent(), 0);
        }
        else if (datatype.type == MPJ.BOOLEAN.type) {
            realcount = BufferOps.bufferBoolean(buf, sendBuf, this.offset, this.count * this.datatype.extent(), 0);
        }
        else if (datatype.type == MPJ.INT.type)  {
            realcount = BufferOps.bufferInt(buf, sendBuf, this.offset, this.count * this.datatype.extent(), 0);
        }
        else if (datatype.type == MPJ.LONG.type) {
            realcount = BufferOps.bufferLong(buf, sendBuf, this.offset, this.count * this.datatype.extent(), 0);
        }
        else if (datatype.type == MPJ.FLOAT.type) {				
            realcount = BufferOps.bufferFloat(buf, sendBuf, this.offset, this.count * this.datatype.extent(), 0);
        }
        else if (datatype.type == MPJ.DOUBLE.type) {
            realcount = BufferOps.bufferDouble(buf, sendBuf, this.offset, this.count * this.datatype.extent(), 0);
        }
        else {
            realcount = BufferOps.bufferObject(buf, sendBuf, this.offset, this.count * this.datatype.extent(), 0);
        }

        synchronized (MPJ.bsendCount) {
            MPJ.bsendCount.bsends--;
            MPJ.bsendCount.notifyAll();
        }



        IbisMPJComm myIbisComm = new IbisMPJComm(this.comm, this.myRank, sendBuf, 0, /*this.count * datatype.extent() * datatype.getByteSize()*/ realcount, this.datatype, this.myRank, this.dest, this.tag, OP_ISEND);
        myIbisComm.buffered = true;
        Request request = myIbisComm.getRequest();
        request.ibisMPJCommThread = new Thread(myIbisComm);
        request.ibisMPJCommThread.start();


    }



    protected void doIssend() throws MPJException {
        doIsend();
    }



    protected void doIrsend() throws MPJException {
        doIsend();
    }

    private void doProbeStandard() throws MPJException {
        //		int typeSize = 1;
        //		int realCount = 0;
        //boolean DEBUG = true;

        IbisIdentifier id = this.comm.group().getId(source);
        Connection con = MPJ.getConnection(id);

        if (DEBUG) {
            System.out.println("iprobe: " + this.comm.rank() + ": " + MPJ.getMyId() + ": SOURCE: " + id + "; rank: " + source);
        }


        MPJObject obj = null;
        ReadMessage msg = null;
        MPJObjectQueue queue = con.getRecvQueue();

        // first check the object queue and hold the monitor on it
        synchronized(queue) {
            queue.lock();
            this.status = queue.probe(this.contextId, this.tag);

            // if queue has not the requested object, try to connect to the receiveport
            if (this.status == null) {

                boolean msgFound = false;

                while(!msgFound) {

                    obj = new MPJObject();
                    msg = con.pollForMessage();

                    if (msg != null) {
                        // 	get message header
                        con.receiveHeader(msg, obj.desc);

                        if(DEBUG) {
                            System.out.println("iprobe tag:       " + obj.getTag());
                            System.out.println("iprobe contextId: " + obj.getContextId());
                            System.out.println("iprobe type:      " + obj.getBaseDatatype());
                            System.out.println("iprobe count:     " + obj.getNumberOfElements());
                            System.out.println("iprobe offset:    " + offset);
                        }

                        obj.initBuffer();
                        con.receiveData(msg, obj.buffer, 0, obj.getNumberOfElements());

                        queue.addObject(obj);


                        // 	the message was expected
                        if (((obj.getTag() == tag) || 
                                    ((tag == MPJ.ANY_TAG) && (obj.getTag() >= 0))) && 
                                (obj.getContextId() == contextId)) {

                            this.status = new Status();
                            this.status.setSource(source);
                            this.status.setTag(obj.getTag());
                            this.status.setCount(obj.getNumberOfElements());
                            this.status.setSize(obj.getNumberOfElements());
                            msgFound = true;
                                }
                        msg = null;
                    }


                    if (this.opCode == OP_IPROBE) {
                        msgFound = true;
                    }
                }

            }
            queue.release();

        }

    }


    private void doProbeAnySource() throws MPJException {
        //		int typeSize = 1;
        //		int realCount = 0;
        //boolean DEBUG = true;


        if (DEBUG) {
            System.out.println("iprobe: " + this.comm.rank() + ": " + MPJ.getMyId() + ": ANY_SOURCE");
        }


        MPJObject obj = null;

        ReadMessage msg = null;

        int src = 0;
        boolean msgFound = false;

        while (!msgFound) {
            if (DEBUG) System.out.println("msg not found");
            IbisIdentifier id = this.comm.group().getId(src);
            Connection con = MPJ.getConnection(id);
            MPJObjectQueue queue = con.getRecvQueue();

            if (queue.isLocked()) {
                src = nextSource(src);

                if (DEBUG) System.out.println("queue is locked! src:" + src);
                continue;
            }

            // first check the object queue and hold the monitor on it
            synchronized(queue) {
                queue.lock();
                this.status = queue.probe(this.contextId, this.tag);

                // if queue has not the requested object, try to connect to the receiveport
                if (this.status == null) {

                    obj = new MPJObject();
                    if (DEBUG) System.out.println("Polling on " + src);
                    msg = con.pollForMessage();

                    if (msg == null) {
                        src = nextSource(src);
                        if (DEBUG) System.out.println("msg was null.");	
                        this.status = null;
                        queue.release();
                        continue;
                    }
                    // 	get message header
                    con.receiveHeader(msg, obj.desc);

                    if(DEBUG) {
                        System.out.println("iprobe tag:       " + obj.getTag());
                        System.out.println("iprobe contextId: " + obj.getContextId());
                        System.out.println("iprobe type:      " + obj.getBaseDatatype());
                        System.out.println("iprobe count:     " + obj.getNumberOfElements());
                        System.out.println("iprobe offset:    " + offset);
                    }

                    obj.initBuffer();
                    con.receiveData(msg, obj.buffer, 0, obj.getNumberOfElements());
                    queue.addObject(obj);

                    // 	the message was expected
                    if (((obj.getTag() == tag) || 
                                ((tag == MPJ.ANY_TAG) && (obj.getTag() >= 0))) && 
                            (obj.getContextId() == contextId)) {
                        this.status = new Status();
                        this.status.setSource(src);
                        this.status.setTag(obj.getTag());
                        this.status.setCount(obj.getNumberOfElements());
                        this.status.setSize(obj.getNumberOfElements());
                        msgFound = true;

                            }
                    msg = null;
                }
                else {
                    msgFound = true;
                    status.setSource(src);
                    queue.release();
                    break;
                }

                queue.release();
            }			
        }
    }


    protected void doIprobe() throws MPJException {


        if (source == MPJ.PROC_NULL) {
            return;
        }

        if(source == MPJ.ANY_SOURCE) {
            doProbeAnySource();
        }
        else {
            doProbeStandard();
        }

    }


    protected boolean isFinished() {
        return(this.finished);
    }



    public void run() {
        this.finished = false;
        if (mode == OP_IRECV) {

            try {
                doIrecv(false);
            }
            catch(MPJException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
                System.exit(-1);
            }
        }
        else if (mode == OP_ISEND) {
            try {
                doIsend();
            }
            catch(MPJException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
                System.exit(-1);
            }
        }
        else if (mode == OP_IBSEND) {
            try {
                doIbsend();				
            }
            catch(MPJException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
                System.exit(-1);
            }
        }
        else if (mode == OP_ISSEND) {
            try {
                doIssend();
            }
            catch(MPJException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
                System.exit(-1);
            }
        }
        else if (mode == OP_IRSEND) {
            try {
                doIrsend();
            }
            catch(MPJException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
                System.exit(-1);
            }
        }

        this.finished = true;
    }





}
