import ibis.ipl.*;

import java.util.Properties;
import java.util.Random;

import java.io.IOException;

interface Config {
    static final boolean tracePortCreation = false;
    static final boolean traceCommunication = false;
    static final boolean showProgress = true;
    static final boolean traceClusterResizing = true;
    static final int BOARDSIZE = 20000;
    static final int GENERATIONS = 10;
}

class RszHandler implements Config, ResizeHandler {
    int members = 0;

    public void join( IbisIdentifier id )
    {
        if( traceClusterResizing ){
            System.err.println( "Join of " + id.name() );
        }
        members++;
    }

    public void leave( IbisIdentifier id )
    {
        if( traceClusterResizing ){
            System.err.println( "Leave of " + id.name() );
        }
        members--;
    }

    public void delete( IbisIdentifier id )
    {
        if( traceClusterResizing ){
            System.err.println( "Delete of " + id );
        }
        members--;
    }

    public void reconfigure()
    {
        if( traceClusterResizing ){
            System.err.println( "Reconfigure" );
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

    private static void usage()
    {
        System.out.println( "Usage: Cell1D [count]" );
        System.exit( 0 );
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
        String portclass;

        if( me<procno ){
            portclass = "Upstream";
        }
        else {
            portclass = "Downstream";
        }
        String sendportname = "send" + portclass + me;
        String receiveportname = "receive" + portclass + procno;

        SendPort res = t.createSendPort( sendportname );
        if( tracePortCreation ){
            System.err.println( "P" + me + ": created send port " + sendportname  );
        }
        ReceivePortIdentifier id = registry.lookup( receiveportname );
        res.connect( id );
        if( tracePortCreation ){
            System.err.println( "P" + me + ": connected " + sendportname + " to " + receiveportname );
        }
        return res;
    }

    /**
     * Creates an update receive port.
     * @param t The type of the port to construct.
     * @param me My own processor number.
     * @param procno The processor to receive from.
     */
    private static ReceivePort createUpdateReceivePort( PortType t, int me, int procno )
        throws java.io.IOException
    {
        String portclass;

        if( me<procno ){
            portclass = "receiveDownstream";
        }
        else {
            portclass = "receiveUpstream";
        }
        String receiveportname = portclass + me;

        ReceivePort res = t.createReceivePort( receiveportname );
        if( tracePortCreation ){
            System.err.println( "P" + me + ": created receive port " + receiveportname  );
        }
        res.enableConnections();
        return res;
    }

    private static byte horTwister[][] = {
        { 0, 0, 0, 0, 0 },
        { 0, 1, 1, 1, 0 },
        { 0, 0, 0, 0, 0 },
    };

    private static byte vertTwister[][] = {
        { 0, 0, 0 },
        { 0, 1, 0 },
        { 0, 1, 0 },
        { 0, 1, 0 },
        { 0, 0, 0 },
    };

    private static byte horTril[][] = {
        { 0, 0, 0, 0, 0, 0 },
        { 0, 0, 1, 1, 0, 0 },
        { 0, 1, 0, 0, 1, 0 },
        { 0, 0, 1, 1, 0, 0 },
        { 0, 0, 0, 0, 0, 0 },
    };

    private static byte vertTril[][] = {
        { 0, 0, 0, 0, 0 },
        { 0, 0, 1, 0, 0 },
        { 0, 1, 0, 1, 0 },
        { 0, 1, 0, 1, 0 },
        { 0, 0, 1, 0, 0 },
        { 0, 0, 0, 0, 0 },
    };

    /**
     * Puts the given pattern at the given coordinates.
     * Since we want the pattern to be readable, we take the first
     * row of the pattern to be the at the top.
     */
    static protected void putPattern( byte board[][], int px, int py, byte pat[][] )
    {
        for( int y=pat.length-1; y>=0; y-- ){
            byte paty[] = pat[y];

            for( int x=0; x<paty.length; x++ ){
                board[px+x][py+y] = paty[x];
            }
        }
    }

    /**
     * Returns true iff the given pattern occurs at the given
     * coordinates.
     */
    static protected boolean hasPattern( byte board[][], int px, int py, byte pat[][ ] )
    {
        for( int y=pat.length-1; y>=0; y-- ){
            byte paty[] = pat[y];

            for( int x=0; x<paty.length; x++ ){
                if( board[px+x][py+y] != paty[x] ){
                    return false;
                }
            }
        }
        return true;
    }

    // Put a twister (a bar of 3 cells) at the given center cell.
    static protected void putTwister( byte board[][], int x, int y )
    {
        putPattern( board, x-2, y-1, horTwister );
    }

    // Given a position, return true iff there is a twister in hor or
    // vertical position at that point.
    static protected boolean hasTwister( byte board[][], int x, int y )
    {
        return hasPattern( board, x-2, y-1, horTwister ) ||
            hasPattern( board, x-1, y-2, vertTwister );
    }

    private static void send( int me, SendPort p, byte data[] )
        throws java.io.IOException
    {
        if( traceCommunication ){
            System.err.println( "P" + me + ": sending from port " + p );
        }
        WriteMessage m = p.newMessage();
        m.writeArray( data );
        m.send();
        m.finish();
    }

    private static void receive( int me, ReceivePort p, byte data[] )
        throws java.io.IOException
    {
        if( traceCommunication ){
            System.err.println( "P" + me + ": receiving on port " + p );
        }
        ReadMessage m = p.receive();
        m.readArray( data );
        m.finish();
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
            count = GENERATIONS;
        }

        try {
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
            final int nProcs = info.size();     // Total number of procs.

            PortType t = ibis.createPortType( "neighbour update", s );

            SendPort leftSendPort = null;
            SendPort rightSendPort = null;
            ReceivePort leftReceivePort = null;
            ReceivePort rightReceivePort = null;

            if( me != 0 ){
                leftReceivePort = createUpdateReceivePort( t, me, me-1 );
            }
            if( me != nProcs-1 ){
                rightReceivePort = createUpdateReceivePort( t, me, me+1 );
            }
            if( me != 0 ){
                leftSendPort = createUpdateSendPort( t, me, me-1 );
            }
            if( me != nProcs-1 ){
                rightSendPort = createUpdateSendPort( t, me, me+1 );
            }

            // The cells. There is a border of cells that are always empty,
            // but make the border conditions easy to handle.
            final int myColumns = BOARDSIZE/nProcs;

            byte oldboard[][] = new byte[myColumns+2][BOARDSIZE+2];
            byte newboard[][] = new byte[myColumns+2][BOARDSIZE+2];

            putTwister( oldboard, 100, 3 );
            if( me == 0 ){
                System.out.println( "Started" );
            }

            byte tmp[][] = oldboard;
            oldboard = newboard;
            newboard = tmp;

            for( int iter=0; iter<count; iter++ ){
                byte prev[];
                byte curr[] = oldboard[0];
                byte next[] = oldboard[1];
                for( int i=1; i<=myColumns; i++ ){
                    prev = curr;
                    curr = next;
                    next = oldboard[i+1];
                    for( int j=1; j<=BOARDSIZE; j++ ){
                        int neighbours =
                            prev[j-1] +
                            prev[j] +
                            prev[j+1] +
                            curr[j-1] +
                            curr[j+1] +
                            next[j-1] +
                            next[j] +
                            next[j+1];
                        boolean alive = (neighbours == 3) || ((neighbours == 2) && (oldboard[i][j]==1));
                        newboard[i][j] = alive?(byte) 1:(byte) 0;
                    }
                }
                if( (me % 2) == 0 ){
                    if( leftSendPort != null ){
                        send( me, leftSendPort, oldboard[1] );
                    }
                    if( rightSendPort != null ){
                        send( me, rightSendPort, oldboard[myColumns] );
                    }
                    if( leftReceivePort != null ){
                        receive( me, leftReceivePort, oldboard[0] );
                    }
                    if( rightReceivePort != null ){
                        receive( me, rightReceivePort, oldboard[myColumns+1] );
                    }
                }
                else {
                    if( rightReceivePort != null ){
                        receive( me, rightReceivePort, oldboard[myColumns+1] );
                    }
                    if( leftReceivePort != null ){
                        receive( me, leftReceivePort, oldboard[0] );
                    }
                    if( rightSendPort != null ){
                        send( me, rightSendPort, oldboard[myColumns] );
                    }
                    if( leftSendPort != null ){
                        send( me, leftSendPort, oldboard[1] );
                    }
                }
                if( showProgress ){
                    if( me == 0 ){
                        System.out.print( '.' );
                    }
                }
            }
            if( showProgress ){
                System.out.println();
            }
            if( !hasTwister( oldboard, 100, 3 ) ){
                System.out.println( "Twister has gone missing" );
            }
            if( me == 0 ){
                System.out.println( "Done" );
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
