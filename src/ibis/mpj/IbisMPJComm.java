/* $Id$ */

/*
 * Created on 04.02.2005
 */
package ibis.mpj;


import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;

import org.apache.log4j.Logger;



/**
 * Implementation of the basic point-to-point communication modes on top of ibis.
 */
public class IbisMPJComm extends Thread {

    static Logger logger = Logger.getLogger(IbisMPJComm.class.getName());

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

        if (logger.isDebugEnabled()) {
            logger.debug("irecv: " + this.comm.rank() + ": " + MPJ.getMyId() + ": ANY_SOURCE");
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
                        if (logger.isDebugEnabled()) {
                            logger.debug("message was null... go to next rank");
                        }
                        continue;
                    }
                    // 	get message header
                    con.receiveHeader(msg, obj.desc);

                    if (logger.isDebugEnabled()) {
                        logger.debug("recv tag:       " + obj.getTag());
                        logger.debug("recv contextId: " + obj.getContextId());
                        logger.debug("recv type:      " + obj.getBaseDatatype());
                        logger.debug("recv count:     " + obj.getNumberOfElements());
                        logger.debug("recv offset:    " + offset);

                        logger.debug("supposed count: " + count * datatype.extent());
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
                            else if (((count*datatype.extent()) < obj.getNumberOfElements()) && (!obj.isBuffered())) {

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
                        if (logger.isDebugEnabled()) {
                            logger.debug("Message was NOT expected -> move to queue");
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
                    if (logger.isDebugEnabled()) {
                        logger.debug("found the object inside the queue");
                    }

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


        IbisIdentifier id = this.comm.group().getId(source);
        Connection con = MPJ.getConnection(id);
        MPJObjectQueue queue = con.getRecvQueue();

        if (logger.isDebugEnabled()) {
            logger.debug("irecv: " + this.comm.rank() + ": " + MPJ.getMyId() + ": SOURCE: " + id + "; rank: " + source);
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
                    if (logger.isDebugEnabled()) {
                        logger.debug("Message was found inside the queue expected -> move out of queue (mytag: " + tag + ")");
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
                    if (logger.isDebugEnabled()) {
                        logger.debug("recv tag:       " + obj.getTag());
                        logger.debug("recv contextId: " + obj.getContextId());
                        logger.debug("recv type:      " + obj.getBaseDatatype());
                        logger.debug("recv count:     " + obj.getNumberOfElements());
                        logger.debug("recv offset:    " + offset);

                        logger.debug("supposed tag:   " + this.tag);
                        logger.debug("supposed contID:" + this.contextId);
                        logger.debug("supposed count: " + count * datatype.extent());
                    }
                    msgReceived = true;

                }		
                // if queue has not the requested object, try to connect to the receiveport
                else if (obj == null) {

                    obj = new MPJObject();
                    msg = null;

                    if (!blocking && queue.size() != 0) {
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

                    if (logger.isDebugEnabled()) {
                        logger.debug("recv tag:       " + obj.getTag());
                        logger.debug("recv contextId: " + obj.getContextId());
                        logger.debug("recv type:      " + obj.getBaseDatatype());
                        logger.debug("recv count:     " + obj.getNumberOfElements());
                        logger.debug("recv offset:    " + offset);

                        logger.debug("supposed tag:   " + this.tag);
                        logger.debug("supposed contID:" + this.contextId);
                        logger.debug("supposed count: " + count * datatype.extent());
                        if (obj.isBuffered()) logger.debug("BUFFERED");
                        else logger.debug("NOT BUFFERED");
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
                            else if ((count*(datatype.extent()+1) < obj.getNumberOfElements()) && (!obj.isBuffered())) {
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
                            if (logger.isDebugEnabled()) {
                                logger.debug("Message was expected and buffered -> unbuffer it");

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
                        if (logger.isDebugEnabled()) {
                            logger.debug("Message was NOT expected -> move to queue");
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

        MPJObject sendObj = new MPJObject(tag, this.contextId, false, datatype.type, count * datatype.extent());
        sendObj.setBuffered(this.buffered);





        if (logger.isDebugEnabled()) {
            logger.debug("send tag:       " + sendObj.getTag());
            logger.debug("send contextId: " + sendObj.getContextId());
            logger.debug("send type:      " + sendObj.getBaseDatatype());
            logger.debug("send count:     " + sendObj.getNumberOfElements());
            logger.debug("send offset:    " + offset);
        }

        try {
            IbisIdentifier id = this.comm.group().getId(dest);
            Connection con = MPJ.getConnection(id);
            if (logger.isDebugEnabled()) {
                logger.debug("isend: " + this.comm.rank() + ": " + MPJ.getMyId() + ": DEST: " + id + "; rank: " + dest + "; tag: " + tag);
            }


            con.putMPJObject(sendObj, buf, offset, count * datatype.extent());
            con.sendMPJObject();



        }
        catch (MPJException e) {
            logger.error("got exception", e);
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

        IbisIdentifier id = this.comm.group().getId(source);
        Connection con = MPJ.getConnection(id);

        if (logger.isDebugEnabled()) {
            logger.debug("iprobe: " + this.comm.rank() + ": " + MPJ.getMyId() + ": SOURCE: " + id + "; rank: " + source);
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

                        if (logger.isDebugEnabled()) {
                            logger.debug("iprobe tag:       " + obj.getTag());
                            logger.debug("iprobe contextId: " + obj.getContextId());
                            logger.debug("iprobe type:      " + obj.getBaseDatatype());
                            logger.debug("iprobe count:     " + obj.getNumberOfElements());
                            logger.debug("iprobe offset:    " + offset);
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

        if (logger.isDebugEnabled()) {
            logger.debug("iprobe: " + this.comm.rank() + ": " + MPJ.getMyId() + ": ANY_SOURCE");
        }


        MPJObject obj = null;

        ReadMessage msg = null;

        int src = 0;
        boolean msgFound = false;

        while (!msgFound) {
            if (logger.isDebugEnabled()) {
                logger.debug("msg not found");
            }
            IbisIdentifier id = this.comm.group().getId(src);
            Connection con = MPJ.getConnection(id);
            MPJObjectQueue queue = con.getRecvQueue();

            if (queue.isLocked()) {
                src = nextSource(src);

                if (logger.isDebugEnabled()) {
                    logger.debug("queue is locked! src:" + src);
                }
                continue;
            }

            // first check the object queue and hold the monitor on it
            synchronized(queue) {
                queue.lock();
                this.status = queue.probe(this.contextId, this.tag);

                // if queue has not the requested object, try to connect to the receiveport
                if (this.status == null) {

                    obj = new MPJObject();
                    if (logger.isDebugEnabled()) {
                        logger.debug("Polling on " + src);
                    }
                    msg = con.pollForMessage();

                    if (msg == null) {
                        src = nextSource(src);
                        if (logger.isDebugEnabled()) {
                            logger.debug("msg was null.");	
                        }
                        this.status = null;
                        queue.release();
                        continue;
                    }
                    // 	get message header
                    con.receiveHeader(msg, obj.desc);

                    if (logger.isDebugEnabled()) {
                        logger.debug("iprobe tag:       " + obj.getTag());
                        logger.debug("iprobe contextId: " + obj.getContextId());
                        logger.debug("iprobe type:      " + obj.getBaseDatatype());
                        logger.debug("iprobe count:     " + obj.getNumberOfElements());
                        logger.debug("iprobe offset:    " + offset);
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

        if (source == MPJ.ANY_SOURCE) {
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
                logger.fatal("got exception", e);
                System.exit(-1);
            }
        }
        else if (mode == OP_ISEND) {
            try {
                doIsend();
            }
            catch(MPJException e) {
                logger.fatal("got exception", e);
                System.exit(-1);
            }
        }
        else if (mode == OP_IBSEND) {
            try {
                doIbsend();				
            }
            catch(MPJException e) {
                logger.fatal("got exception", e);
                System.exit(-1);
            }
        }
        else if (mode == OP_ISSEND) {
            try {
                doIssend();
            }
            catch(MPJException e) {
                logger.fatal("got exception", e);
                System.exit(-1);
            }
        }
        else if (mode == OP_IRSEND) {
            try {
                doIrsend();
            }
            catch(MPJException e) {
                logger.fatal("got exception", e);
                System.exit(-1);
            }
        }

        this.finished = true;
    }





}
