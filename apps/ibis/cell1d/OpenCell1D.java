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
    static final boolean traceClusterResizing = false;
    static final boolean traceLoadBalancing = true;
    static final int DEFAULTBOARDSIZE = 4000;
    static final int GENERATIONS = 30;
    static final int SHOWNBOARDWIDTH = 60;
    static final int SHOWNBOARDHEIGHT = 30;
}

final class Problem implements OpenConfig {
    public byte leftBorder[];
    public byte board[][];
    public byte rightBorder[];
    public int firstColumn = -1;
    public int firstNoColumn = -1;

    // We need two extra column arrays to temporarily store the update
    // of a column. These arrays will be circulated with our columns of
    // the board.
    public byte updatecol[];
    public byte nextupdatecol[];

    public Problem( int boardsize, int firstCol, int firstNoCol )
    {
        // We allocate one column extra to allow a more efficient computational
        // loop. The last element will always be null, though.
        board = new byte[boardsize+1][];
        updatecol = new byte[boardsize+2];
        nextupdatecol = new byte[boardsize+2];
        firstColumn = firstCol;
        firstNoColumn = firstNoCol;

        // Now populate the columns that are our responsibility,
        for( int col=firstCol; col<firstNoCol; col++ ){
            board[col] = new byte[boardsize+2];
        }

        // And two border columns.
        leftBorder = new byte[boardsize+2];
        rightBorder = new byte[boardsize+2];
    }
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
           OpenCell1D.leftNeighbour = prev;
           if( traceClusterResizing ){
               String who = "no";

               if( OpenCell1D.leftNeighbour != null ){
                   who = OpenCell1D.leftNeighbour.name() + " as";
               }
               System.out.println( "P" + members + ": that's me! I have " + who + " left neighbour" );
           }
           OpenCell1D.me = members;
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
            System.out.println( "Cluster is reconfigured" );
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
    static int generation = -1;
    static int boardsize = DEFAULTBOARDSIZE;
    static boolean idle = true;
    static boolean rightNeighbourIdle = true;
    static int aimFirstColumn;
    static int aimFirstNoColumn;
    static int knownMembers = 0;
    static RszHandler rszHandler = new RszHandler();

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
            System.out.println( "P" + me + ": created send port " + res  );
        }
        ReceivePortIdentifier id = registry.lookup( receiveportname );
        res.connect( id );
        if( tracePortCreation ){
            System.out.println( "P" + me + ": connected " + sendportname + " to " + receiveportname );
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
            System.out.println( "P" + me + ": created receive port " + res  );
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
    static protected void putPattern( Problem p, int px, int py, byte pat[][] )
    {
        for( int y=pat.length-1; y>=0; y-- ){
            byte paty[] = pat[y];

            for( int x=0; x<paty.length; x++ ){
                if( p.board[px+x] != null ){
                    p.board[px+x][py+y] = paty[x];
                }
            }
        }
    }

    /**
     * Returns true iff the given pattern occurs at the given
     * coordinates.
     */
    static protected boolean hasPattern( Problem p, int px, int py, byte pat[][ ] )
    {
        for( int y=pat.length-1; y>=0; y-- ){
            byte paty[] = pat[y];

            for( int x=0; x<paty.length; x++ ){
                if( p.board[px+x] != null && p.board[px+x][py+y] != paty[x] ){
                    return false;
                }
            }
        }
        return true;
    }

    // Put a twister (a bar of 3 cells) at the given center cell.
    static protected void putTwister( Problem p, int x, int y )
    {
        putPattern( p, x-2, y-1, horTwister );
    }

    // Given a position, return true iff there is a twister in hor or
    // vertical position at that point.
    static protected boolean hasTwister( Problem p, int x, int y )
    {
        return hasPattern( p, x-2, y-1, horTwister ) ||
            hasPattern( p, x-1, y-2, vertTwister );
    }

    /**
     * Sends a new border to the lefthand neighbour. For load balancing
     * purposes, perhaps also send some of the columns I own to that
     * neighbour.
     * @param port The port to send to.
     * @param p The problem.
     * @param aimFirstColomn The first column we should own.
     * @param aimFirstNoColomn The first column we should not own.
     */
    static void sendLeft( SendPort port, Problem p, int aimFirstColumn, int aimFirstNoColumn )
        throws java.io.IOException
    {
        if( port == null ){
            if( traceCommunication ){
                System.out.println( "P" + me + ":" + generation + ": there is no left neighbour to send to" );
            }
            return;
        }
        if( traceCommunication ){
            System.out.println( "P" + me + ":" + generation + ": sending to P" + (me-1) + ": " + port );
        }
        if( p.firstColumn == 0 ){
            System.err.println( "ERROR: I have a left neighbour, but my first column is 0???" );
            System.exit( 1 );
        }
        int sendCount;
        if( p.firstColumn<boardsize ){
            sendCount = aimFirstColumn-p.firstColumn;
            if( sendCount<0 ){
                sendCount = 0;
            }
        }
        else {
            sendCount = 0;
        }
        if( sendCount>0 ){
            if( traceLoadBalancing ){
                System.out.println( "P" + me + ":" + generation + ": sending " + sendCount + " columns to P" + (me+1) );
            }
            // The border has changed, but since until now we maintained it,
            // we can record its current state from our own columns.
            System.arraycopy( p.board[aimFirstColumn-1], 0, p.leftBorder, 0, boardsize+2 );
        }
        WriteMessage m = port.newMessage();
        m.writeInt( generation );
        m.writeInt( sendCount );

        // Send the columns we want to move to the border.
        while( sendCount>0 ){
            int ix = p.firstColumn;
            byte buf[] = p.board[ix];

            if( buf == null ){
                // This shouldn't happen, but make the best of it.
                System.out.println( "ERROR: P" + me + ":" + generation + ": cannot send null column " + ix + " to P" + (me-1) + "; sending a dummy instead" );
                buf = p.leftBorder;
            }
            m.writeInt( ix );
            m.writeArray( buf );
            p.board[ix] = null;
            p.firstColumn++;
            sendCount--;
        }

        // ... and always send our first column as border to
        // the neighbour.
        m.writeInt( p.firstColumn );
        if( p.firstColumn<p.firstNoColumn ){
            if( traceCommunication ){
                System.out.println( "P" + me + ":" + generation + ": sending border column " + p.firstColumn + " to P" + (me-1) );
            }
            m.writeArray( p.board[p.firstColumn] );
        }
        else {
            if( traceCommunication ){
                System.out.println( "P" + me + ":" + generation + ": there is no border column to send to P" + (me-1) );
            }
        }
        m.send();
        m.finish();
    }

    /**
     * Sends a new border to the righthand neighbour. For load balancing
     * purposes, perhaps also send some of the columns I own to that
     * neighbour.
     * @param port The port to send to.
     * @param p The problem.
     * @param aimFirstColomn The first column we should own.
     * @param aimFirstNoColomn The first column we should not own.
     */
    static void sendRight( SendPort port, Problem p, int aimFirstColumn, int aimFirstNoColumn )
        throws java.io.IOException
    {
        if( port == null ){
            if( traceCommunication ){
                System.out.println( "P" + me + ":" + generation + ": there is no right neighbour to send to" );
            }
            return;
        }
        if( traceCommunication ){
            System.out.println( "P" + me + ":" + generation + ": sending to P" + (me+1) +": " + port );
        }
        int sendCount;
        if( p.firstColumn<boardsize ){
            int currentAimFirstNoColumn = aimFirstNoColumn;

            // Make sure we have at least two columns left.
            if( currentAimFirstNoColumn<p.firstColumn+2 ){
                currentAimFirstNoColumn = p.firstColumn+2;
                if( traceLoadBalancing ){
                    System.out.println( "P" + me + ":" + generation + ": P" + (me+1) + " cannot get all my columns, I keep 2" );
                }
            }
            sendCount = p.firstNoColumn-currentAimFirstNoColumn;
            if( sendCount<0 ){
                sendCount = 0;
            }
        }
        else {
            sendCount = 0;
        }
        if( sendCount>0 ){
            rightNeighbourIdle = false;

            if( traceLoadBalancing ){
                System.out.println( "P" + me + ":" + generation + ": sending " + sendCount + " columns to P" + (me+1) + " (" + (p.firstNoColumn-sendCount) + "-" + p.firstNoColumn + ")" );
            }
            // The border has changed, but since until now we
            // maintained it as an ordinary column, we can easily intialize
            // it.
            System.arraycopy( p.board[p.firstNoColumn-sendCount], 0, p.rightBorder, 0, boardsize+2 );
        }
        WriteMessage m = port.newMessage();
        m.writeInt( generation );
        m.writeInt( sendCount );

        // Send the columns we want to move to our right neighbour.
        while( sendCount>0 ){
            int ix = p.firstNoColumn-1;
            byte buf[] = p.board[ix];

            if( buf == null ){
                // This shouldn't happen, but make the best of it.
                System.out.println( "ERROR: P" + me + ":" + generation + ": cannot send null column " + ix + " to P" + (me+1) + "; sending a dummy instead" );
                buf = p.leftBorder;
            }
            m.writeInt( ix );
            m.writeArray( buf );
            p.board[ix] = null;
            p.firstNoColumn--;
            sendCount--;
        }

        // TODO: make sure that all this shrinking doesn't leave us with
        // an empty set, unless all our right neighbours are also
        // empty.

        // ... and always send our first column as border to the neighbour.
        if( p.firstColumn<p.firstNoColumn ){
            int ix = p.firstNoColumn-1;
            m.writeInt( ix );
            if( traceCommunication ){
                System.out.println( "P" + me + ":" + generation + ": sending border column " + ix + " to P" + (me+1) );
            }
            byte buf[] = p.board[ix];
            if( buf == null ){
                System.out.println( "ERROR: P" + me + ":" + generation + ": cannot send right border column " + ix + " since it is null; sending a dummy" );
                buf = p.rightBorder;
            }
            m.writeArray( buf );
        }
        else {
            // We don't have columns, so no border columns either. Tell
            // that to the opposite side.
            if( traceCommunication ){
                System.out.println( "P" + me + ":" + generation + ": there is no border column to send to P" + (me+1) );
            }
            m.writeInt( boardsize );
        }
        m.send();
        m.finish();
    }

    static void receiveLeft( ReceivePort port, Problem p )
        throws java.io.IOException
    {
        int colno;

        if( port == null ){
            if( traceCommunication ){
                System.out.println( "P" + me + ":" + generation + ": there is no left neighbour to receive from" );
            }
            return;
        }
        if( traceCommunication ){
            System.out.println( "P" + me + ":" + generation + ": receiving from P" + (me-1) + ": " + port );
        }
        ReadMessage m = port.receive();
        int gen = m.readInt();
        if( gen>=0 && generation<0 ){
            generation = gen;
        }
        int receiveCount = m.readInt();
        if( receiveCount>0 ){
            if( traceLoadBalancing ){
                System.out.println( "P" + me + ":" + generation + ": receiving " + receiveCount + " columns from P" + (me-1) );
            }
            int newFirst = p.firstColumn;
            int newLast = -1;
            for( int i=0; i<receiveCount; i++ ){
                colno = m.readInt();

                if( colno>=p.firstColumn && colno<p.firstNoColumn ){
                    System.out.println( "ERROR: P" + me + ": left neighbour P" + (me-1) + " sent column " + colno + ", but that is in my range" );
                }
                else if( p.board[colno] != null ){
                    System.out.println( "ERROR: P" + me + ": left neighbour P" + (me-1) + " sent column " + colno + ", but I already have a column there (although it is not in my range)" );
                }
                byte buf[] = new byte[boardsize+2];

                m.readArray( buf );
                p.board[colno] = buf;
                if( colno<newFirst ){
                    newFirst = colno;
                }
                if( colno>newLast ){
                    newLast = colno;
                }
            }
            if( p.firstColumn>=boardsize ){
                p.firstNoColumn = newLast+1;
            }
            else {
                if( (newLast+1)<p.firstColumn ){
                    System.out.println( "ERROR: P" + me + ": left neighbour P" + (me-1) + " sent columns " + newFirst + "-" + (newLast+1) + " but that leaves a gap to my columns " + p.firstColumn + "-" + p.firstNoColumn );
                }
            }
            p.firstColumn = newFirst;
        }
        colno = m.readInt();
        if( colno<boardsize ){
            // TODO: check that the column number is the one we expect.
            if( traceCommunication ){
                System.out.println( "P" + me + ":" + generation + ": receiving border column " + colno + " from P" + (me-1) );
            }
            m.readArray( p.leftBorder );
        }
        else {
            if( traceCommunication ){
                System.out.println( "P" + me + ":" + generation + ": P" + (me-1) + " doesn't have a border column to send" );
            }
        }
        m.finish();
        if( traceCommunication ){
            System.out.println( "P" + me + ":" + generation + ": completed receiving from P" + (me-1) );
        }
    }

    static void receiveRight( ReceivePort port, Problem p )
        throws java.io.IOException
    {
        int colno;

        if( port == null ){
            if( traceCommunication ){
                System.out.println( "P" + me + ":" + generation + ": there is no right neighbour to receive from" );
            }
            return;
        }
        if( traceCommunication ){
            System.out.println( "P" + me + ":" + generation + ": receiving from P" + (me+1) + ": " + port );
        }
        ReadMessage m = port.receive();
        int gen = m.readInt();
        if( gen>=0 && generation<0 ){
            generation = gen;
        }
        int receiveCount = m.readInt();
        if( receiveCount>0 ){
            if( traceLoadBalancing ){
                System.out.println( "P" + me + ":" + generation + ": receiving " + receiveCount + " columns from P" + (me+1) );
            }
        }
        p.firstNoColumn += receiveCount;
        int ix = p.firstNoColumn;

        for( int i=0; i<receiveCount; i++ ){
            ix--;
            if( p.board[ix] == null ){
                p.board[ix] = new byte[boardsize+2];
            }
            else {
                System.out.println( "P" + me + ":" + generation + ": column " + ix + " is not in my posession, but is not null" );
            }
            colno = m.readInt();
            if( colno != ix ){
                System.out.println( "ERROR: P" + me +":" + generation + ": P" + (me+1) + " sent me column " + colno + ", but I need column " + ix );
            }
            m.readArray( p.board[ix] );
        }
        colno = m.readInt();
        if( colno<boardsize ){
            // TODO: check that the column number is the one we expect.
            if( traceCommunication ){
                System.out.println( "P" + me + ":" + generation + ": receiving border column " + colno + " from P" + (me+1) );
            }
            m.readArray( p.rightBorder );
        }
        else {
            if( traceCommunication ){
                System.out.println( "P" + me + ":" + generation + ": P" + (me+1) + " doesn't have a border column to send" );
            }
        }
        m.finish();
        if( traceCommunication ){
            System.out.println( "P" + me + ":" + generation + ": completed receiving from P" + (me+1) );
        }
    }

    static void computeNextGeneration( Problem p )
    {
        if( p.firstColumn<p.firstNoColumn ){
            byte prev[];
            byte curr[] = p.leftBorder;
            byte next[] = p.board[p.firstColumn];

            if( showBoard && leftNeighbour == null ){
                System.out.println( "Generation " + generation );
                for( int y=0; y<SHOWNBOARDHEIGHT; y++ ){
                    for( int x=1; x<SHOWNBOARDWIDTH; x++ ){
                        System.out.print( p.board[x][y] );
                    }
                    System.out.println();
                }
            }
            for( int i=p.firstColumn; i<p.firstNoColumn; i++ ){
                prev = curr;
                curr = next;
                next = p.board[i+1];
                if( next == null ){
                    // No column there. We blindly assume that
                    // that means we must use the right border.
                    next = p.rightBorder;
                }
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
                    boolean alive = (neighbours == 3) || ((neighbours == 2) && (curr[j]==1));
                    p.updatecol[j] = alive?(byte) 1:(byte) 0;
                }
                
                //
                byte tmp[] = p.board[i];
                p.board[i] = p.updatecol;
                p.updatecol = p.nextupdatecol;
                p.nextupdatecol = tmp;
            }
        }
    }

    /**
     * See if any new members have joined the computation, and if so
     * update the column numbers we should try to own.
     */
    static void updateAims( Problem p )
    {
        int members = rszHandler.getMemberCount();
        if( knownMembers<members ){
            // Some processors have joined the computation.
            // Redistribute the load.

            // For an equal division of the load, I should get...
            aimFirstColumn = (me*boardsize)/members;
            aimFirstNoColumn = ((me+1)*boardsize)/members;

            // Take into account that each node should have at least
            // two columns.
            if( aimFirstColumn<2*me ){
                aimFirstColumn = 2*me;
            }
            if( aimFirstNoColumn<aimFirstColumn+2 ){
                aimFirstNoColumn = aimFirstColumn+2;
            }
            if( traceLoadBalancing ){
                System.out.println( "P" + me + ":" + generation + ": there are now " + members + " nodes in the computation (was " + knownMembers + ")" );
                if( p.firstColumn>=p.firstNoColumn ){
                    System.out.println( "P" + me + ": I am idle, I should have columns " + aimFirstColumn + "-" + aimFirstNoColumn );
                }
                else {
                    System.out.println( "P" + me + ": I have columns " + p.firstColumn + "-" + p.firstNoColumn + ", I should have " + aimFirstColumn + "-" + aimFirstNoColumn );
                }
            }
            knownMembers = members;
        }
    }

    public static void main( String [] args )
    {
        int count = GENERATIONS;

        /** The first column that is my responsibility. */
        int firstColumn = -1;

        /** The first column that is no longer my responsibility. */
        int firstNoColumn = -1;

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

            if( me != 0 && leftNeighbour == null ){
                System.out.println( "P" + me + ": there is no left neighbour???" );
            }
            if( leftNeighbour != null ){
                leftReceivePort = createNeighbourReceivePort( updatePort, "upstream" );
                leftSendPort = createNeighbourSendPort( updatePort, leftNeighbour, "downstream" );
            }

            if( leftNeighbour == null ){
                // I'm the leftmost node, I start with the entire board.
                // Workstealing will spread the load to other processors later
                // on.
                firstColumn = 0;
                firstNoColumn = boardsize;
                generation = 0; // I decide the generation count.
                knownMembers = 1;
                idle = false;
            }
            else {
                firstColumn = boardsize;
                firstNoColumn = boardsize;
            }

            if( me == 0 ){
                System.out.println( "Started" );
            }

            // For the moment we're satisfied with the work distribution.
            aimFirstColumn = firstColumn;
            aimFirstNoColumn = firstNoColumn;

            // First, create an array to hold all columns of the total
            // array size, plus two empty dummy border columns. (The top and
            // bottom *rows* are also empty dummies that are never updated).
            Problem p = new Problem( boardsize, firstColumn, firstNoColumn );

            // Put a few fixed objects on the board to do a sanity check.
            putTwister( p, 3, 100 );
            putPattern( p, 4, 4, glider );

            if( idle ){
                // Waiting for work.
                if( traceLoadBalancing ){
                    System.out.println( "P" + me + ": ready and waiting for work" );
                }
                receiveRight( rightReceivePort, p );
            }

            while( generation<count ){
                if( rightNeighbour != null && rightReceivePort == null ){
                    // We now have a right neightbour. Set up communication
                    // with it.
                    if( tracePortCreation ){
                        System.out.println( "P" + me + ": a right neighbour has appeared; creating ports" );
                    }
                    rightReceivePort = createNeighbourReceivePort( updatePort, "downstream" );
                    rightSendPort = createNeighbourSendPort( updatePort, rightNeighbour, "upstream" );
                }
                updateAims( p );
                if( rightNeighbourIdle && rightSendPort != null && aimFirstNoColumn<p.firstNoColumn ){
                    // We have some work for our lazy right neighbour.
                    // give him the good news.
                    if( traceLoadBalancing ){
                        System.out.println( "P" + me + ":" + generation + ": sending work to idle neighbour P" + (me+1) );
                    }
                    sendRight( rightSendPort, p, aimFirstColumn, aimFirstNoColumn );
                    rightNeighbourIdle = false;
                }
                computeNextGeneration( p );
                generation++;
                updateAims( p );
                if( (me % 2) == 0 ){
                    if( !rightNeighbourIdle ){
                        sendRight( rightSendPort, p, aimFirstColumn, aimFirstNoColumn );
                        receiveRight( rightReceivePort, p );
                    }
                    sendLeft( leftSendPort, p, aimFirstColumn, aimFirstNoColumn );
                    receiveLeft( leftReceivePort, p );
                }
                else {
                    receiveLeft( leftReceivePort, p );
                    sendLeft( leftSendPort, p, aimFirstColumn, aimFirstNoColumn );
                    if( !rightNeighbourIdle ){
                        receiveRight( rightReceivePort, p );
                        sendRight( rightSendPort, p, aimFirstColumn, aimFirstNoColumn );
                    }
                }
                if( showProgress && me == 0 ){
                    System.out.print( '.' );
                }
            }
            if( showProgress && me == 0 ){
                System.out.println();
            }

            // Do a sanity check.
            if( !hasTwister( p, 3, 100 ) ){
                System.out.println( "Twister has gone missing" );
            }

            // TODO: also do a sanity check on the other pattern we
            // put on the board.

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
