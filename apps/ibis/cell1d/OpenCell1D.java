// File: $Id$

import ibis.ipl.*;

import java.util.Properties;
import java.util.Random;

import java.io.IOException;

interface OpenConfig {
    static final boolean tracePortCreation = false;
    static final boolean traceCommunication = false;
    static final boolean showProgress = true;
    static final boolean showBoard = false;
    static final boolean traceClusterResizing = true;
    static final boolean traceLoadBalancing = true;
    static final int DEFAULTBOARDSIZE = 3000;
    static final int GENERATIONS = 100;
    static final int SHOWNBOARDWIDTH = 60;
    static final int SHOWNBOARDHEIGHT = 30;
}

class RszHandler implements OpenConfig, ResizeHandler {
    private int members = 0;
    private IbisIdentifier prev = null;

    public void join( IbisIdentifier id )
    {
        if( traceClusterResizing ){
            System.out.println( "Machine " + id.name() + " joins the computation" );
        }
        if( id.equals( OpenCell1D.ibis.identifier() ) ){
           // Hey! That's me. Now I know my member number and my left
           // neighbour.
           OpenCell1D.me = members;
           OpenCell1D.leftNeighbour = prev;
           if( traceClusterResizing ){
               String who = "no";

               if( OpenCell1D.leftNeighbour != null ){
                   who = OpenCell1D.leftNeighbour.name() + " as";
               }
               System.out.println( "P" + OpenCell1D.me + ": that's me! I have " + who + " left neighbour" );
           }
        }
        else if( prev != null && prev.equals( OpenCell1D.ibis.identifier() ) ){
            // The next one after me. Now I know my right neighbour.
            OpenCell1D.rightNeighbour = id;
            if( traceClusterResizing ){
                System.out.println( "P" + OpenCell1D.me + ": that's my right neighbour" );
            }
        }
        members++;
        prev = id;
    }

    public void leave( IbisIdentifier id )
    {
        if( traceClusterResizing ){
            System.out.println( "Machine " + id.name() + " leaves the computation" );
        }
        members--;
    }

    public void delete( IbisIdentifier id )
    {
        if( traceClusterResizing ){
            System.out.println( "Machine " + id.name() + " is deleted from the computation" );
        }
        members--;
    }

    public void reconfigure()
    {
        if( traceClusterResizing ){
            System.out.println( "Reconfigure" );
        }
    }

    public synchronized int getMemberCount()
    {
        return members;
    }
}

class OpenCell1D implements OpenConfig {
    static Ibis ibis;
    static Registry registry;
    static IbisIdentifier leftNeighbour;
    static IbisIdentifier rightNeighbour;
    static IbisIdentifier myName;
    static int me = -1;
    static SendPort leftSendPort;
    static SendPort rightSendPort;
    static ReceivePort leftReceivePort;
    static ReceivePort rightReceivePort;
    static int generation = 0;
    static int boardsize = DEFAULTBOARDSIZE;

    /** The first column that is my responsibility. */
    static int firstColumn = -1;

    /** The first column that is no longer my responsibility. */
    static int firstNoColumn = -1;

    private static void usage()
    {
        System.out.println( "Usage: OpenCell1D [-size <int>] [count]" );
        System.exit( 0 );
    }

    /**
     * Creates an update send port that connected to the specified neighbour.
     * @param updatePort The type of the port to construct.
     * @param dest The destination processor.
     * @param prefix The prefix of the port names.
     */
    private static SendPort createNeighbourSendPort( PortType updatePort, IbisIdentifier dest, String prefix )
        throws java.io.IOException
    {
        String sendportname = prefix + "Send" + myName.name();
        String receiveportname = prefix + "Receive" + dest.name();

        SendPort res = updatePort.createSendPort( sendportname );
        if( tracePortCreation ){
            System.out.println( "P" + OpenCell1D.me + ": created send port " + sendportname  );
        }
        ReceivePortIdentifier id = registry.lookup( receiveportname );
        res.connect( id );
        if( tracePortCreation ){
            System.out.println( "P" + OpenCell1D.me + ": connected " + sendportname + " to " + receiveportname );
        }
        return res;
    }

    /**
     * Creates an update receive port.
     * @param updatePort The type of the port to construct.
     * @param prefix The prefix of the port names.
     */
    private static ReceivePort createNeighbourReceivePort( PortType updatePort, String prefix )
        throws java.io.IOException
    {
        String receiveportname = prefix + "Receive" + myName.name();

        ReceivePort res = updatePort.createReceivePort( receiveportname );
        if( tracePortCreation ){
            System.out.println( "P" + OpenCell1D.me + ": created receive port " + receiveportname  );
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

    private static byte glider[][] = {
        { 0, 0, 0, 0, 0 },
        { 0, 1, 1, 1, 0 },
        { 0, 1, 0, 0, 0 },
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
                if( board[px+x] != null ){
                    board[px+x][py+y] = paty[x];
                }
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
                if( board[px+x] != null && board[px+x][py+y] != paty[x] ){
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

    /**
     * @param p The port to send to.
     * @param firstColumn The first column to send.
     * @param firstNoColumn The first column not to send.
     * @param board The game board.
     */
    private static void send( SendPort p, int firstColumn, int firstNoColumn, byte board[][] )
        throws java.io.IOException
    {
        if( board[firstColumn] == null ){
            System.err.println( "P" + me + ": cannot send a null array to " + p );
            return;
        }
        if( traceCommunication ){
            System.out.println( myName.name() + ": sending from port " + p );
        }
        WriteMessage m = p.newMessage();
        m.writeInt( OpenCell1D.generation );
        m.writeInt( firstColumn );
        m.writeInt( firstNoColumn );
        for( int i=firstColumn; i<firstNoColumn; i++ ){
            m.writeArray( board[i] );
        }
        m.send();
        m.finish();
    }

    /**
     * @param p The port to receive on.
     * @param board The game board.
     */
    private static void receive( ReceivePort p, byte board[][] )
        throws java.io.IOException
    {
        if( traceCommunication ){
            System.out.println( myName.name() + ": receiving on port " + p );
        }
        ReadMessage m = p.receive();
        int gen = m.readInt();
        if( gen>=0 && OpenCell1D.generation<0 ){
            OpenCell1D.generation = gen;
        }
        int firstCol = m.readInt();
        int firstNoCol = m.readInt();
        for( int i=firstCol; i<firstNoCol; i++ ){
            if( board[i] == null ){
                board[i] = new byte[OpenCell1D.boardsize+2];
            }
            m.readArray( board[i] );
        }
        m.finish();
    }

    public static void main( String [] args )
    {
        int count = GENERATIONS;
        int rank = 0;
        int remoteRank = 1;
        boolean noneSer = false;
        RszHandler rszHandler = new RszHandler();
        int knownMembers = 0;

        /* Parse commandline parameters. */
        for( int i=0; i<args.length; i++ ){
            if( args[i].equals( "-size" ) ){
                i++;
                boardsize = Integer.parseInt( args[i] );
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

        try {
            long startTime = System.currentTimeMillis();

            StaticProperties s = new StaticProperties();
            s.add( "serialization", "data" );
            s.add( "communication", "OneToOne, Reliable, AutoUpcalls, ExplicitReceipt" );
            s.add( "worldmodel", "open" );
            ibis = Ibis.createIbis( s, rszHandler );
            myName = ibis.identifier();

            ibis.openWorld();

            registry = ibis.registry();

            // TODO: be more precise about the properties for the two
            // port types.
            PortType updatePort = ibis.createPortType( "neighbour update", s );
            PortType loadbalancePort = ibis.createPortType( "loadbalance", s );

            leftSendPort = null;
            rightSendPort = null;
            leftReceivePort = null;
            rightReceivePort = null;

            // Wait until I know my processor number (and also
            // my left neighbour).

            // TODO: use a more subtle approach than this.
            while( me<0 ){
                Thread.sleep( 20 );
            }

            if( leftNeighbour != null ){
                leftReceivePort = createNeighbourReceivePort( updatePort, "upstream" );
            }
            if( leftNeighbour != null ){
                leftSendPort = createNeighbourSendPort( updatePort, leftNeighbour, "downstream" );
            }

            if( leftNeighbour == null ){
                // I'm the leftmost node, I start with the entire board.
                // Workstealing will spread the load to other processors later
                // on.
                firstColumn = 1;
                firstNoColumn = boardsize+1;
                generation = 0; // I decide the generation count.
                knownMembers = 1;
            }
            else {
                firstColumn = boardsize+1;
                firstNoColumn = boardsize+1;
            }

            // For the moment we're satisfied with the work distribution.
            int aimFirstColumn = firstColumn;
            int aimFirstNoColumn = firstNoColumn;

            // First, create an array to hold all columns of the total
            // array size, plus two empty dummy border columns. (The top and
            // bottom *rows* are also empty dummies that are never updated).
            byte board[][] = new byte[boardsize+2][];

            // We need two extra column arrays to temporarily store the update
            // of a column. These arrays will be circulated with our columns of
            // the board.
            byte updatecol[] = new byte[boardsize+2];
            byte nextupdatecol[] = new byte[boardsize+2];

            if( me == 0 ){
                System.out.println( "Started" );
            }

            // Now populate the columns that are our responsibility,
            // plus two border columns.
            for( int col=firstColumn-1; col<=firstNoColumn; col++ ){
                board[col] = new byte[boardsize+2];
            }

            putTwister( board, 100, 3 );
            putPattern( board, 4, 4, glider );

            while( generation<count ){
                if( firstColumn<firstNoColumn ){
                    byte prev[];
                    byte curr[] = board[firstColumn];
                    byte next[] = board[firstColumn+1];

                    if( showBoard && leftNeighbour == null ){
                        System.out.println( "Generation " + generation );
                        for( int y=1; y<SHOWNBOARDHEIGHT; y++ ){
                            for( int x=1; x<SHOWNBOARDWIDTH; x++ ){
                                System.out.print( board[x][y] );
                            }
                            System.out.println();
                        }
                    }
                    for( int i=firstColumn; i<firstNoColumn; i++ ){
                        prev = curr;
                        curr = next;
                        next = board[i+1];
                        for( int j=1; j<=boardsize; j++ ){
                            int neighbours =
                                prev[j-1] +
                                prev[j] +
                                prev[j+1] +
                                curr[j-1] +
                                curr[j+1] +
                                next[j-1] +
                                next[j] +
                                next[j+1];
                            boolean alive = (neighbours == 3) || ((neighbours == 2) && (board[i][j]==1));
                            updatecol[j] = alive?(byte) 1:(byte) 0;
                        }
                        
                        //
                        byte tmp[] = board[i];
                        board[i] = updatecol;
                        updatecol = nextupdatecol;
                        nextupdatecol = tmp;
                    }
                }
                if( rightNeighbour != null ){
                    if( rightReceivePort == null ){
                        if( tracePortCreation ){
                            System.out.println( "P" + me + ": a right neighbour has appeared; creating ports" );
                        }
                        rightReceivePort = createNeighbourReceivePort( updatePort, "downstream" );
                        rightSendPort = createNeighbourSendPort( updatePort, rightNeighbour, "upstream" );
                    }
                }
                int mem = rszHandler.getMemberCount();
                if( knownMembers<mem ){
                    // Some processors have joined the computation.
                    aimFirstColumn = 1+(me*boardsize)/mem;
                    aimFirstNoColumn = 1+((me+1)*boardsize)/mem;
                    if( traceLoadBalancing ){
                        System.out.println( "P" + me + ": there are now " + mem + " nodes in the computation (was " + knownMembers + ")" );
                        System.out.println( "P" + me + ": I have columns " + firstColumn + " to " + firstNoColumn );
                        System.out.println( "P" + me + ": I should have columns " + aimFirstColumn + " to " + aimFirstNoColumn );
                    }
                    knownMembers = mem;
                }
                if( (me % 2) == 0 ){
                    if( leftSendPort != null ){
                        if( aimFirstColumn>firstColumn ){
                            if( traceLoadBalancing ){
                                System.out.println( "P" + me + ": I should send columns " + firstColumn + " to " + aimFirstColumn + " to my left neighbour" );
                            }
                        }
                        send( leftSendPort, firstColumn, firstColumn+1, board );
                    }
                    if( rightSendPort != null ){
                        if( aimFirstNoColumn>=firstColumn && aimFirstNoColumn<firstNoColumn ){
                            if( traceLoadBalancing ){
                                System.out.println( "P" + me + ": I should send columns " + aimFirstNoColumn + " to " + firstNoColumn + " to my right neighbour" );
                            }
                        }
                        send( rightSendPort, firstNoColumn-1, firstNoColumn, board );
                    }
                    if( leftReceivePort != null ){
                        receive( leftReceivePort, board );
                    }
                    if( rightReceivePort != null ){
                        receive( rightReceivePort, board );
                    }
                }
                else {
                    if( rightReceivePort != null ){
                        receive( rightReceivePort, board );
                    }
                    if( leftReceivePort != null ){
                        receive( leftReceivePort, board );
                    }
                    if( rightSendPort != null ){
                        if( aimFirstNoColumn>=firstColumn && aimFirstNoColumn<firstNoColumn ){
                            if( traceLoadBalancing ){
                                System.out.println( "P" + me + ": I should send columns " + aimFirstNoColumn + " to " + firstNoColumn + " to my right neighbour" );
                            }
                        }
                        send( rightSendPort, firstNoColumn-1, firstNoColumn, board );
                    }
                    if( leftSendPort != null ){
                        if( aimFirstColumn>firstColumn ){
                            if( traceLoadBalancing ){
                                System.out.println( "P" + me + ": I should send columns " + firstColumn + " to " + aimFirstColumn + " to my left neighbour" );
                            }
                        }
                        send( leftSendPort, firstColumn, firstColumn+1, board );
                    }
                }
                if( showProgress ){
                    if( leftNeighbour == null ){
                        System.out.print( '.' );
                    }
                }
                generation++;
            }
            if( showProgress ){
                if( leftNeighbour == null ){
                    System.out.println();
                }
            }
            if( !hasTwister( board, 100, 3 ) ){
                System.out.println( "Twister has gone missing" );
            }
            if( me == 0 ){
                long endTime = System.currentTimeMillis();
                double time = ((double) (endTime - startTime))/1000.0;
                long updates = boardsize*boardsize*(long) count;

                System.out.println( "ExecutionTime: " + time );
                System.out.println( "Did " + updates + " updates" );
            }

            ibis.end();
        }
        catch( Exception e ) {
            System.out.println( "Got exception " + e );
            System.out.println( "StackTrace:" );
            e.printStackTrace();
        }
    }
}
