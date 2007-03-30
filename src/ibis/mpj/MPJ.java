/* $Id$ */

/*
 * Created on 21.01.2005
 */
package ibis.mpj;


import ibis.ipl.CapabilitySet;
import ibis.ipl.Ibis;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.PredefinedCapabilities;
import ibis.ipl.Registry;

/**
 * Main MPJ class.
 */
public class MPJ implements PredefinedCapabilities {
    protected static boolean LOCALCOPYIBIS = true;



    private static final boolean DEBUG = false;
    private static byte[] attachedBuffer;
    private static boolean isInitialized = false;
    private static int highestContextId = -1;
    private static PortType porttype = null;
    private static ConnectionTable connectionTable = null;

    protected static Ibis ibis;
    protected static Registry registry;
    private static RegEvtHandler regEvtHandler;


    public static Datatype BYTE, CHAR, SHORT, BOOLEAN, INT, LONG, FLOAT, DOUBLE, OBJECT, PACKED, LB, UB;
    public static Datatype SHORT2, INT2, LONG2, FLOAT2, DOUBLE2;
    public static Op MAX, MIN, SUM, PROD, LAND, BAND, LOR, BOR, LXOR, BXOR, MAXLOC, MINLOC;


    public static Group GROUP_EMPTY;
    public static Intracomm COMM_WORLD;
    public static Comm COMM_SELF;

    public static final int ANY_SOURCE = -777;
    public static final int ANY_TAG    = -666;

    public static final int PROC_NULL = -1;

    public static final int UNDEFINED = -1;
    public static final int IDENT	  = 0;
    public static final int SIMILAR	  = 1;
    public static final int CONGRUENT = 2;
    public static final int UNEQUAL   = 3;
    public static final int CART 	  = 4;
    public static final int GRAPH	  = 5;

    public static final int TAG_UB = -1;
    public static final int HOST = -2;
    public static final int IO = -3;
    public static final int WTIME_IS_GLOBAL = -4;

    private static void initDatatypes() {

        BYTE    = new Datatype();
        BYTE.byteSize = 1;
        BYTE.type = Datatype.BASE_TYPE_BYTE;
        BYTE.displacements = new int[1];
        BYTE.lb = 0;
        BYTE.ub = 0;
        BYTE.hasMPJLB = false;
        BYTE.hasMPJUB = false;


        CHAR    = new Datatype();
        CHAR.byteSize = 2;
        CHAR.type = Datatype.BASE_TYPE_CHAR;
        CHAR.displacements = new int[1];
        CHAR.lb = 0;
        CHAR.ub = 0;
        CHAR.hasMPJLB = false;
        CHAR.hasMPJUB = false;

        SHORT   = new Datatype();
        SHORT.byteSize = 2;
        SHORT.type = Datatype.BASE_TYPE_SHORT;
        SHORT.displacements = new int[1];
        SHORT.lb = 0;
        SHORT.ub = 0;
        SHORT.hasMPJLB = false;
        SHORT.hasMPJUB = false;

        BOOLEAN = new Datatype();
        BOOLEAN.byteSize = 1;
        BOOLEAN.type = Datatype.BASE_TYPE_BOOLEAN;
        BOOLEAN.displacements = new int[1];
        BOOLEAN.lb = 0;
        BOOLEAN.ub = 0;
        BOOLEAN.hasMPJLB = false;
        BOOLEAN.hasMPJUB = false;

        INT     = new Datatype();
        INT.byteSize = 4;
        INT.type = Datatype.BASE_TYPE_INT;
        INT.displacements = new int[1];
        INT.lb = 0;
        INT.ub = 0;
        INT.hasMPJLB = false;
        INT.hasMPJUB = false;

        LONG    = new Datatype();
        LONG.byteSize = 8;
        LONG.type = Datatype.BASE_TYPE_LONG;
        LONG.displacements = new int[1];
        LONG.lb = 0;
        LONG.ub = 0;
        LONG.hasMPJLB = false;
        LONG.hasMPJUB = false;

        FLOAT   = new Datatype();
        FLOAT.byteSize = 4;
        FLOAT.type = Datatype.BASE_TYPE_FLOAT;
        FLOAT.displacements = new int[1];
        FLOAT.lb = 0;
        FLOAT.ub = 0;
        FLOAT.hasMPJLB = false;
        FLOAT.hasMPJUB = false;

        DOUBLE  = new Datatype();
        DOUBLE.byteSize = 8;
        DOUBLE.type = Datatype.BASE_TYPE_DOUBLE;
        DOUBLE.displacements = new int[1];
        DOUBLE.lb = 0;
        DOUBLE.ub = 0;
        DOUBLE.hasMPJLB = false;
        DOUBLE.hasMPJUB = false;

        OBJECT  = new Datatype();
        OBJECT.byteSize = 1;
        OBJECT.type = Datatype.BASE_TYPE_OBJECT;
        OBJECT.displacements = new int[1];    	
        OBJECT.lb = 0;
        OBJECT.ub = 0;
        OBJECT.hasMPJLB = false;
        OBJECT.hasMPJUB = false;

        PACKED  = new Datatype();
        PACKED.byteSize = 1;
        PACKED.type = Datatype.BASE_TYPE_BYTE;
        PACKED.displacements = new int[1];    	
        PACKED.lb = 0;
        PACKED.ub = 0;
        PACKED.hasMPJLB = false;
        PACKED.hasMPJUB = false;

        LB  = new Datatype();
        LB.byteSize = -1;
        LB.type = Datatype.BASE_TYPE_OBJECT;
        LB.displacements = new int[1];    	
        LB.lb = 0;
        LB.ub = 0;
        LB.hasMPJLB = true;
        LB.hasMPJUB = false;

        UB  = new Datatype();
        UB.byteSize = -1;
        UB.type = Datatype.BASE_TYPE_OBJECT;
        UB.displacements = new int[1];    	
        UB.lb = 0;
        UB.ub = 0;
        UB.hasMPJLB = false;
        UB.hasMPJUB = true;

        try {
            SHORT2 = SHORT.contiguous(2);
            INT2 = INT.contiguous(2);
            LONG2 = LONG.contiguous(2);
            FLOAT2 = FLOAT.contiguous(2);
            DOUBLE2 = DOUBLE.contiguous(2);

        }
        catch (MPJException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }


    }

    private static void initOps() {
        try {

            MAX  = new OpMax(true);
            MIN  = new OpMin(true);
            SUM  = new OpSum(true);
            PROD = new OpProd(true);
            LAND = new OpLand(true);
            BAND = new OpBand(true);
            LOR  = new OpLor(true);
            BOR  = new OpBor(true);
            LXOR = new OpLxor(true);
            BXOR = new OpBxor(true);
            MAXLOC = new OpMaxLoc(true);
            MINLOC = new OpMinLoc(true);
        }
        catch (MPJException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }

    }


    /**
     * Initialize MPJ.
     * 
     * @param args arguments to main method
     * @throws MPJException
     */
    public static void init(String[] args) throws MPJException {
        try {

            int k = 0;
            while (k < args.length) {
                if (args[k].equals("mpj.localcopySun")) {
                    MPJ.LOCALCOPYIBIS = false;
                }
                else if (args[k].endsWith("mpj.localcopyIbis")) {
                    MPJ.LOCALCOPYIBIS = true;
                }
                k++;
            }

            regEvtHandler = new RegEvtHandler();

            CapabilitySet s = new CapabilitySet(
                SERIALIZATION_OBJECT, COMMUNICATION_RELIABLE, RECEIVE_EXPLICIT,
                RECEIVE_POLL, CONNECTION_ONE_TO_ONE, WORLDMODEL_CLOSED,
                RESIZE_UPCALLS);
            ibis = IbisFactory.createIbis(s, null, null, regEvtHandler);

            regEvtHandler.waitForEveryone(ibis);

            registry = ibis.registry();

            CapabilitySet prop = new CapabilitySet(
                SERIALIZATION_OBJECT, COMMUNICATION_RELIABLE, RECEIVE_EXPLICIT,
                CONNECTION_ONE_TO_ONE, RECEIVE_POLL);

            porttype = ibis.createPortType(prop);


            if (DEBUG) {
                System.err.println("My Id is " + ibis.identifier());
                System.err.println("MyRank is " + regEvtHandler.myRank + " of " + regEvtHandler.nInstances + "\n");
                System.err.println("establishing communication...");
            }

            COMM_WORLD = new Intracomm();
            COMM_WORLD.group = new Group();

            //MPJ.upcall = new MPJUpcall(MPJQueue);

            connectionTable = new ConnectionTable();
            for (int i = 0; i < MPJ.regEvtHandler.nInstances; i++) {

                Connection connection = new Connection(registry, porttype, MPJ.regEvtHandler.myRank, i);
                connection.setupReceivePort();
                connectionTable.addConnection(MPJ.regEvtHandler.identifiers[i], connection);
                COMM_WORLD.group().addId(MPJ.regEvtHandler.identifiers[i]);
            }

            registry.elect("" + MPJ.regEvtHandler.myRank);

            for (int i = 0; i < MPJ.regEvtHandler.nInstances; i++) {
                Connection con = MPJ.connectionTable.getConnection(MPJ.regEvtHandler.identifiers[i]);
                con.setupSendPort();
            }

            // Wait until communication is established
            boolean check = false;

            while (!check) {
                check = true;
                for (int i=0; i<MPJ.regEvtHandler.nInstances; i++) {
                    Connection con = MPJ.connectionTable.getConnection(MPJ.regEvtHandler.identifiers[i]);
                    if (!con.isConnectionEstablished()) {
                        check = false;
                        break;
                    }
                }
            }

            COMM_SELF = new Comm();
            COMM_SELF.group = new Group();
            COMM_SELF.group().addId(ibis.identifier());
            //COMM_SELF.contextId = 0;


            initDatatypes();
            initOps();







            MPJ.isInitialized = true;

            if (DEBUG) {
                System.err.println("MPJ initialized.");
            }

        }
        catch( Exception e ) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }


    }

    /**
     * Finalize MPJ.
     * 
     * @throws MPJException
     */
    public static void finish() throws MPJException {

        MPJ.isInitialized = false;

        try {
            if (DEBUG) {
                System.err.println("try to finish up...");
            }


            for (int i=0; i<MPJ.regEvtHandler.nInstances; i++) {
                IbisIdentifier mpjId = MPJ.COMM_WORLD.group().getId(i);
                Connection con = MPJ.connectionTable.getConnection(mpjId);
                con.close();
            }


            ibis.end();

            if (DEBUG) {
                System.err.println("MPJ finished.");
            }


        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();

        }
    }

    /**
     * Returns the MPJ name of the processor on which it is called.
     * 
     * @return A unique specifier for the actual node
     * @throws MPJException
     */
    public static String getProcessorName() throws MPJException {
        return ibis.identifier().toString();
    }

    /**
     * Returns wallclock time
     * 
     * @return elapsed wallclock time in seconds since some time in the past
     * @throws MPJException
     */
    public static double wtime() throws MPJException {
        return(System.currentTimeMillis() / 1000.0);
    }




    /**
     * Returns resolution of the timer.
     * 
     * @return resolution of wtime in seconds
     */
    // taken from LAM_MPI implementation
    public static double wtick() {

        //		double resolution = 0.0001;
        double tick = 0.0;
        double t;

        if ( tick == 0.0 ) {
            tick = System.currentTimeMillis();
            tick = System.currentTimeMillis() - tick;

            for (int counter = 0; counter < 10; counter++) {
                t = System.currentTimeMillis();
                t = System.currentTimeMillis() - t;

                if ( t < tick ) {
                    tick = t;
                }
            }

            tick = (tick > 0.0) ? tick : 1.0e-6;

        }
        return(tick);
    }

    /**
     * Test if MPJ has been initialized.
     * 
     * @return true if init has been called, false otherwise
     * @throws MPJException
     */
    public static boolean initialized() throws MPJException {
        return MPJ.isInitialized;
    }




    /**
     * Provides to MPJ a buffer in user's memory to be used for buffering outgoing messages.
     * @param buffer buffer array
     */
    public static void bufferAttach(byte[] buffer) {
        attachedBuffer = buffer;
    }

    protected static BsendCount bsendCount = new BsendCount();
    /**
     * Detach the buffer currently associated with MPJ.
     * @return buffer array
     */
    public static byte[] bufferDetach() {

        synchronized (bsendCount) {

            while(bsendCount.bsends != 0) {
                try {
                    bsendCount.wait();
                }
                catch(InterruptedException e) {
                	// ignored
                }
            }

            byte[] detBuffer = new byte[attachedBuffer.length];
            System.arraycopy(attachedBuffer, 0, detBuffer, 0, attachedBuffer.length);
            attachedBuffer = null;

            return(detBuffer);
        }
    }




    protected static byte[] getAttachedBuffer() {
        return(attachedBuffer);
    }


    protected static IbisIdentifier getMyId() {
        return ibis.identifier();
    }

    protected static Connection getConnection(IbisIdentifier id) throws MPJException {
        return(MPJ.connectionTable.getConnection(id));
    }

    protected static int getNewContextId() {
        return (MPJ.highestContextId+1);
    }

    protected static void setNewContextId(int contextId) throws MPJException {
        if (contextId > MPJ.highestContextId) {
            MPJ.highestContextId = contextId;
        }
        else {
            throw new MPJException("Context ID: " + contextId + " is already in use or not allowed.");
        }
    }
}
class BsendCount {
    protected int bsends = 0;
}
