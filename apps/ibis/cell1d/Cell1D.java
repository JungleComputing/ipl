import ibis.ipl.*;

import java.util.Properties;
import java.util.Random;

import java.io.IOException;

interface Config {
    static final boolean DEBUG = false;
}

class RszHandler implements ResizeHandler {
    int members = 0;

    public void join( IbisIdentifier id )
    {
        System.err.println( "Join of " + id.name() );
        members++;
    }

    public void leave( IbisIdentifier id )
    {
        System.err.println( "Leave of " + id.name() );
        members--;
    }

    public void delete( IbisIdentifier id )
    {
        System.err.println( "Delete of " + id );
    }

    public void reconfigure()
    {
        System.err.println( "Reconfigure" );
    }
}

class Sender implements Config
{
    SendPort sport;
    ReceivePort rport;

    Sender( ReceivePort rport, SendPort sport ) {
        this.rport = rport;
        this.sport = sport;
    }

    void send( int count, int repeat ) throws Exception
    {
        for( int r=0; r<repeat; r++ ) {
            long time = System.currentTimeMillis();

            for( int i = 0; i< count; i++ ) {
                WriteMessage writeMessage = sport.newMessage();
                if( DEBUG ) {
                    System.out.println( "LAT: finish message" );
                }
                writeMessage.finish();
                if( DEBUG ) {
                    System.out.println( "LAT: message done" );
                }
                ReadMessage readMessage = rport.receive();
                readMessage.finish();
            }

            time = System.currentTimeMillis() - time;

            double speed = (time * 1000.0) / (double) count;
            System.err.println( "Latency: " + count + " calls took " + ( time/1000.0 ) + " seconds, time/call = " + speed + " micros" );
        }
    }
}

class Receiver implements Config {

	SendPort sport;
	ReceivePort rport;

	Receiver( ReceivePort rport, SendPort sport ) {
            this.rport = rport;
            this.sport = sport;
	}

	void receive( int count, int repeat ) throws IOException {
            for( int r=0; r<repeat; r++ ) {
                for( int i = 0; i< count; i++ ) {
                    if( DEBUG ) {
                        System.out.println( "LAT: in receive" );
                    }
                    ReadMessage readMessage = rport.receive();
                    if( DEBUG ) {
                        System.out.println( "LAT: receive done" );
                    }
                    readMessage.finish();
                    if( DEBUG ) {
                        System.out.println( "LAT: finish done" );
                    }

                    WriteMessage writeMessage = sport.newMessage();
                    writeMessage.finish();
                }
            }
	}
}

class Cell1D implements Config {
    static Ibis ibis;
    static Registry registry;
    static ibis.util.PoolInfo info;

    public static void connect( SendPort s, ReceivePortIdentifier ident ) {
        boolean success = false;
        do {
            try {
                s.connect( ident );
                success = true;
            }
            catch( Exception e ) {
                try {
                    Thread.sleep( 500 );
                }
                catch( Exception e2 ) {
                    // ignore
                }
            }
        } while( !success );
    }

    public static ReceivePortIdentifier lookup( String name ) throws Exception {
        ReceivePortIdentifier temp = null;

        do {
            temp = registry.lookup( name );

            if( temp == null ) {
                try {
                    Thread.sleep( 500 );
                }
                catch( Exception e ) {
                    // ignore
                }
            }

        } while( temp == null );

        return temp;
    }

    private static void usage()
    {
        System.out.println( "Usage: Cell1D [count]" );
        System.exit( 0 );
    }

    /**
     * Given the name of a receive port, return its identifier.
     */
    private static ReceivePortIdentifier findReceivePort( String name )
        throws java.io.IOException
    {
        ReceivePortIdentifier id = null;

        while( id == null ){
            id = registry.lookup( name );

            if( id == null ){
                try {
                    Thread.sleep( 1000 );
                }
                catch( Exception e ){
                    // Ignore.
                    // TODO: do somethingg a bit more subtle.
                }
            }
        }
        return id;
    }

    /**
     * Creates an update send port that connected to the specified neighbour.
     * @param t The type of the port to construct.
     * @param me My own processor number.
     * @param procno The processor number to connect to.
     */
    private static SendPort createUpdateSendPort( PortType t, int me, int procno )
        throws java.io.IOException
    {
        SendPort res = t.createSendPort( "update" + me );
        ReceivePortIdentifier id = findReceivePort( "update" + procno );
        connect( res, id );
        return res;
    }

    /**
     * Creates an update receive port.
     * @param t The type of the port to construct.
     * @param me My own processor number.
     */
    private ReceivePort createUpdateReceivePort( PortType t, int me, int procno )
        throws java.io.IOException
    {
        ReceivePort res = t.createReceivePort( "update" + me );
        return res;
    }

    public static void main( String [] args )
    {
        int count = -1;
        int repeat = 10;
        int rank = 0;
        int remoteRank = 1;
        boolean noneSer = false;
        RszHandler rszHandler = new RszHandler();

        /* Parse commandline parameters. */
        for( int i=0; i<args.length; i++ ){
            if( args[i].equals( "-repeat" ) ){
                i++;
                repeat = Integer.parseInt( args[i] );
            }
            else {
                if( count == -1 ){
                    count = Integer.parseInt( args[i] );
                }
                else {
                    usage();
                }
            }
        }

        if( count == -1 ) {
            count = 10000;
        }

        try {
            String portprops = "OneToOne, Reliable, ExplicitReceipt";

            info = new ibis.util.PoolInfo();
            StaticProperties s = new StaticProperties();
            s.add( "serialization", "data" );
            s.add( "communication", "OneToOne, Reliable, ExplicitReceipt" );
            s.add( "worldmodel", "closed" );
            ibis = Ibis.createIbis( s, rszHandler );

            // ibis.openWorld();

            registry = ibis.registry();

            // This only works for a closed world...
            final int me = info.rank();         // My processor number.
            final int NProcs = info.size();     // Total number of procs.

            System.err.println( "Me=" + me + ", NProcs=" + NProcs );

            PortType t = ibis.createPortType( "neighbour update", s );

            SendPort leftSendPort = null;
            SendPort rightSendPort = null;
            ReceivePort leftReceivePort = null;
            ReceivePort rightReceivePort = null;

            if( me != 0 ){
                leftReceivePort = t.createReceivePort( "send port" );
                leftSendPort = t.createSendPort( "send port" );
            }
            if( me != NProcs-1 ){
                rightReceivePort = t.createReceivePort( "send port" );
                rightSendPort = t.createSendPort( "send port" );
            }
            ReceivePort rport;
            Cell1D lat = null;

            if( DEBUG ) {
                System.out.println( "LAT: pre elect" );
            }
            ibis.end();
        }
        catch( Exception e ) {
            System.err.println( "Got exception " + e );
            System.err.println( "StackTrace:" );
            e.printStackTrace();
        }
    }
}
