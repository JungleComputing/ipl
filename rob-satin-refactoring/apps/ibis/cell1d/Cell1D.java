// File: $Id$

import ibis.ipl.*;

import java.util.Properties;
import java.util.Random;
import ibis.util.PoolInfo;

import java.io.IOException;

interface Config {
    static final boolean tracePortCreation = false;
    static final boolean traceCommunication = false;
    static final boolean showProgress = false;
    static final boolean showBoard = false;
    static final int GENERATIONS = 100;
    static final int SHOWNBOARDWIDTH = 60;
    static final int SHOWNBOARDHEIGHT = 30;
}

class Cell1D implements Config {
    static Ibis ibis;
    static Registry registry;
    static PoolInfo info;
    static int boardsize = 3000;

    // We need two extra column arrays to temporarily store the update
    // of a column. These arrays will be circulated with the columns of
    // the board.
    static byte updatecol[];
    static byte nextupdatecol[];

    private static void usage()
    {
        System.out.println( "Usage: Cell1D [-size <n>] [count]" );
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
        ReceivePortIdentifier id = registry.lookupReceivePort( receiveportname );
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

    static void computeNextGeneration( byte board[][], int iter, final int myColumns )
    {
        byte prev[];
        byte curr[] = board[0];
        byte next[] = board[1];

        for( int i=1; i<=myColumns; i++ ){
            prev = curr;
            curr = next;
            next = board[i+1];
            for( int j=1; j<=boardsize; j++ ){
                updatecol[j] = Ecology.computeNextState(
                    iter,
                    i, j,
                    prev[j-1],
                    prev[j],
                    prev[j+1],
                    curr[j-1],
                    curr[j],
                    curr[j+1],
                    next[j-1],
                    next[j],
                    next[j+1]
                );
            }
            
            //
            byte tmp[] = board[i];
            board[i] = updatecol;
            updatecol = nextupdatecol;
            nextupdatecol = tmp;
        }
    }

    public static void main( String [] args )
    {
        int count = GENERATIONS;
        int repeat = 10;
        int rank = 0;
        int remoteRank = 1;
        boolean noneSer = false;

        /* Parse commandline parameters. */
        for( int i=0; i<args.length; i++ ){
            if( args[i].equals( "-size" ) ){
                i++;
                boardsize = Integer.parseInt( args[i] );
            }
            else {
                count = Integer.parseInt( args[i] );
            }
        }

        try {
            info = PoolInfo.createPoolInfo();
            StaticProperties s = new StaticProperties();
            s.add( "serialization", "data" );
            s.add( "communication", "OneToOne, Reliable, ExplicitReceipt" );
            s.add( "worldmodel", "closed" );
            ibis = Ibis.createIbis( s, null );

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

            final int myColumns = boardsize/nProcs;

            // The cells. There is a border of cells that are always empty,
            // but make the border conditions easy to handle.
            byte board[][] = new byte[myColumns+2][boardsize+2];

            updatecol = new byte[boardsize+2];
            nextupdatecol = new byte[boardsize+2];

            if( me == 0 ){
                System.out.println( Helpers.getPlatformVersion() );
                System.out.println( "Using " + ibis.implementationName() );
                System.out.println( "Started" );
            }

            if( false ){
                Life.putTwister( board, 3, 100 );
                Life.putGlider( board, 4, 4 );
            }
            else {
                Ecology.putForest( board, 100, 100 );
            }

            long startTime = System.currentTimeMillis();

            for( int iter=0; iter<count; iter++ ){

                if( showBoard && me == 0 ){
                    System.out.println( "Generation " + iter );
                    for( int y=1; y<SHOWNBOARDHEIGHT; y++ ){
                        for( int x=1; x<SHOWNBOARDWIDTH; x++ ){
                            System.out.print( board[x][y] );
                        }
                        System.out.println();
                    }
                }
                computeNextGeneration( board, iter, myColumns );
                if( (me % 2) == 0 ){
                    if( leftSendPort != null ){
                        send( me, leftSendPort, board[1] );
                    }
                    if( rightSendPort != null ){
                        send( me, rightSendPort, board[myColumns] );
                    }
                    if( leftReceivePort != null ){
                        receive( me, leftReceivePort, board[0] );
                    }
                    if( rightReceivePort != null ){
                        receive( me, rightReceivePort, board[myColumns+1] );
                    }
                }
                else {
                    if( rightReceivePort != null ){
                        receive( me, rightReceivePort, board[myColumns+1] );
                    }
                    if( leftReceivePort != null ){
                        receive( me, leftReceivePort, board[0] );
                    }
                    if( rightSendPort != null ){
                        send( me, rightSendPort, board[myColumns] );
                    }
                    if( leftSendPort != null ){
                        send( me, leftSendPort, board[1] );
                    }
                }
                if( showProgress ){
                    if( me == 0 ){
                        System.out.print( '.' );
                    }
                }
            }
            if( showProgress ){
                if( me == 0 ){
                    System.out.println();
                }
            }
            if( me == 0 ){
                long endTime = System.currentTimeMillis();
                double time = ((double) (endTime - startTime))/1000.0;
                long updates = boardsize*boardsize*(long) count;

                System.out.println( "ExecutionTime: " + time );
                System.out.println( "Did " + updates + " updates" );
            }

            if( leftSendPort != null ){
                leftSendPort.close();
            }
            if( rightSendPort != null ){
                rightSendPort.close();
            }
            if( leftReceivePort != null ){
                leftReceivePort.close();
            }
            if( rightReceivePort != null ){
                rightReceivePort.close();
            }
            if( false && !Life.hasTwister( board, 3, 100 ) ){
                System.out.println( "Twister has gone missing" );
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
