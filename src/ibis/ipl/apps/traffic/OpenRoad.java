package ibis.ipl.apps.traffic;

// File: $Id$

import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.MessageUpcall;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.Registry;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

interface OpenConfig {
    static final boolean tracePortCreation = false;
    static final boolean traceGenerations = false;
    static final boolean traceCommunication = false;
    static final boolean showProgress = false;
    static final boolean showBoard = false;
    static final boolean traceClusterResizing = false;
    static final boolean traceLoadBalancing = false;
    static final boolean traceWorkStealing = false;
    static final boolean doWorkStealing = true;
    static final int DISTURBANCE = 0;
    static final int DISTURBANCE_START = 0;
    static final int DEFAULTBOARDSIZE = 4000;
    static final int DEFAULTGENERATIONS = 30;
    static final int SHOWNBOARDWIDTH = 60;
    static final int SHOWNBOARDHEIGHT = 30;
}

final class LockedInt {
    private int v;

    public LockedInt(){ v = 0; }
    public LockedInt( int n ){ v = n; }

    public synchronized int get() { return v; }

    public synchronized void set( int n ) { v = n; }
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
        // We use a null column as guard element for the righthand border
        // of the columns of each processor. To provide room for this guard
        // column for the last processor, we allocate one extra column.
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

class RszHandler implements OpenConfig, RegistryEventHandler {
    private int members = 0;
    private IbisIdentifier prev = null;

    public void joined( IbisIdentifier id )
    {
        if( traceClusterResizing ){
            System.out.println( "Machine " + id + " joins the computation" );
        }
        if( id.equals( OpenCell1D.ibis.identifier() ) ){
           // Hey! That's me. Now I know my member number and my left
           // neighbour.
           OpenCell1D.leftNeighbour = prev;
           if( traceClusterResizing ){
               String who = "no";

               if( OpenCell1D.leftNeighbour != null ){
                   who = OpenCell1D.leftNeighbour + " as";
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

    public void left( IbisIdentifier id )
    {
        if( traceClusterResizing ){
            System.out.println( "Machine " + id + " leaves the computation" );
        }
        members--;
    }

    public void died( IbisIdentifier id )
    {
        if( traceClusterResizing ){
            System.out.println( "Machine " + id + " died" );
        }
        members--;
    }

    public void gotSignal( String signal ) {
        // We don't do this.
    }

    public synchronized int getMemberCount()
    {
        return members;
    }
}

class LevelRecorder implements MessageUpcall {
    int level = -1;

    public LevelRecorder(){}

    public synchronized int get() { return level; }

    public synchronized void reset() { level = -1; }

    public void upcall( ReadMessage m ) throws java.io.IOException
    {
        int gen = m.readInt();
        int mygen = OpenCell1D.lockedGeneration.get();
        if( mygen == gen ){
            level = OpenCell1D.boardsize-OpenCell1D.column.get();
        }
        else if( gen>mygen ){
            System.err.println( "Steal request generation: " + gen + "; my generation: " + mygen );
        }
        m.finish();
    }
}

class OpenCell1D implements OpenConfig {
    static Ibis ibis;
    static Registry registry;
    static IbisIdentifier leftNeighbour;
    static IbisIdentifier rightNeighbour;
    static IbisIdentifier myName;
    static int me = -1;

    static SendPort leftSendPort = null;
    static SendPort rightSendPort = null;
    static ReceivePort leftReceivePort = null;
    static ReceivePort rightReceivePort = null;

    static SendPort leftStealSendPort = null;
    static SendPort rightStealSendPort = null;
    static ReceivePort leftStealReceivePort = null;
    static ReceivePort rightStealReceivePort = null;
    static LevelRecorder leftRecorder = new LevelRecorder();
    static LevelRecorder rightRecorder = new LevelRecorder();

    static int generation = -1;
    static int boardsize = DEFAULTBOARDSIZE;
    static boolean idle = true;
    static boolean rightNeighbourIdle = true;
    static int aimFirstColumn;
    static int aimFirstNoColumn;
    static int knownMembers = 0;
    static RszHandler rszHandler = new RszHandler();
    static final int minLoad = 2;
    static LockedInt column = new LockedInt();
    static LockedInt lockedGeneration = new LockedInt( 0 );

    // Arrays to record statistical information for each generation. Can be
    // null if we're not interested.
    static int population[] = null;
    static int members[] = null;
    static int sentToLeft[] = null;
    static int sentToRight[] = null;
    static long computationTime[] = null;
    static long communicationTime[] = null;
    static long administrationTime[] = null;
    static int requestedByLeft[] = null;
    static int requestedByRight[] = null;

    static int max_lsteal = 0;
    static int max_rsteal = 0;

    /**
     * Creates an update send port that connected to the specified neighbour.
     * @param updatePort The type of the port to construct.
     * @param dest The destination processor.
     * @param prefix The prefix of the port names.
     */
    private static SendPort createNeighbourSendPort( PortType updatePort, IbisIdentifier dest, String prefix )
        throws java.io.IOException
    {
        String sendportname = prefix + "Send";
        String receiveportname = prefix + "Receive";

        SendPort res = ibis.createSendPort( updatePort, sendportname );
        if( tracePortCreation ){
            System.out.println( "P" + me + ": created send port " + res  );
        }
        res.connect( dest, receiveportname );
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
    private static ReceivePort createNeighbourReceivePort( PortType updatePort, String prefix, MessageUpcall up )
        throws java.io.IOException
    {
        String receiveportname = prefix + "Receive";

        ReceivePort res;
        if( up == null ){
            res = ibis.createReceivePort( updatePort, receiveportname );
        }
        else {
            res = ibis.createReceivePort( updatePort, receiveportname, up );
        }
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

    @SuppressWarnings("unused")
    private static byte horTril[][] = {
        { 0, 0, 0, 0, 0, 0 },
        { 0, 0, 1, 1, 0, 0 },
        { 0, 1, 0, 0, 1, 0 },
        { 0, 0, 1, 1, 0, 0 },
        { 0, 0, 0, 0, 0, 0 },
    };

    @SuppressWarnings("unused")
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
     * @param aimFirstColumn The first column we should own.
     * @param aimFirstNoColumn The first column we should not own.
     */
    static void sendToLeft( SendPort port, Problem p, int aimFirstColumn, int aimFirstNoColumn )
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
        if( sentToLeft != null ){
            sentToLeft[generation] = sendCount;
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

        // Send the columns we want to move to our neighbour.
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
     * @param aimFirstColumn The first column we should own.
     * @param aimFirstNoColumn The first column we should not own.
     */
    static void sendToRight( SendPort port, Problem p, int aimFirstColumn, int aimFirstNoColumn )
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
        if( sentToRight != null ){
            sentToRight[generation] = sendCount;
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

    /**
     * @param port The port to receive from.
     * @param p The problem to update.
     */
    static void receiveFromLeft( ReceivePort port, Problem p )
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
            if( traceGenerations ){
                System.out.println( "P" + me + ": set generation counter to " + generation + " based on packet from P" + (me-1) );
            }
        }
        int receiveCount = m.readInt();
        if( receiveCount>0 ){
            max_lsteal = 0;
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
            aimFirstColumn = newFirst;
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

    /**
     * @param port The port to receive from.
     * @param p The problem to update.
     */
    static void receiveFromRight( ReceivePort port, Problem p )
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
            if( traceGenerations ){
                System.out.println( "P" + me + ": set generation counter to " + generation + " based on packet from P" + (me+1) );
            }
        }
        int receiveCount = m.readInt();
        if( receiveCount>0 ){
            max_rsteal = 0;
            if( traceLoadBalancing ){
                System.out.println( "P" + me + ":" + generation + ": receiving " + receiveCount + " columns from P" + (me+1) );
            }
        }
        p.firstNoColumn += receiveCount;
        aimFirstNoColumn += receiveCount;
        int ix = p.firstNoColumn-receiveCount;

        for( int i=0; i<receiveCount; i++ ){
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
            ix++;
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

    /**
     * Sends a steal request on the given port. The steal message contains
     * the generation number to allow the receiver to determine whether
     * the request is stale.
     * @param port The port to send the steal request on.
     * @param generation The generation we've just completed.
     */
    static void sendStealRequest( SendPort port, int generation )
        throws java.io.IOException
    {
        if( port == null ){
            return;
        }
        WriteMessage m = port.newMessage();
        m.writeInt( generation );
        m.send();
        m.finish();
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
            for( int computeColumn=p.firstColumn; computeColumn<p.firstNoColumn; computeColumn++ ){
                column.set( computeColumn );
                prev = curr;
                curr = next;
                next = p.board[computeColumn+1];
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
                if( DISTURBANCE>0 && (me == 1) && generation>=DISTURBANCE_START ){
                    for( int iters=0; iters<3; iters++ ){
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
                    }
                }
                
                byte tmp[] = p.board[computeColumn];
                p.board[computeColumn] = p.updatecol;
                p.updatecol = p.nextupdatecol;
                p.nextupdatecol = tmp;
            }
        }
    }

    /**
     * See if any new members have joined the computation, and if so
     * update the column numbers we should try to own.
     */
    static void updateMembership( Problem p )
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
            if( aimFirstColumn<minLoad*me ){
                aimFirstColumn = minLoad*me;
            }
            if( aimFirstNoColumn<aimFirstColumn+minLoad ){
                aimFirstNoColumn = aimFirstColumn+minLoad;
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

    private static void evaluateStealRequests( Problem p, int lsteal, int rsteal )
    {
        if( aimFirstColumn != p.firstColumn && aimFirstNoColumn != p.firstNoColumn ){
            max_lsteal = 1;
            max_rsteal = 1;
            return;
        }
        double dampen = 0.3;

        if( lsteal>0 && rsteal>0 ){
            // Don't give away our work to *both* neighbours at the
            // same time.
            if( lsteal>rsteal ){
                rsteal = 0;
            }
            else if( rsteal>lsteal ){
                lsteal = 0;
            }
            else {
                dampen *= 2;
            }
        }
        if( lsteal>0 ){
            // The left neighbour needs columns the most, send them.
            int stolen = (int) (dampen*lsteal);
            aimFirstColumn += Math.min( stolen, max_lsteal );
            if( aimFirstColumn+minLoad>aimFirstNoColumn ){
                aimFirstColumn = aimFirstNoColumn-minLoad;
            }
            max_lsteal = 1+Math.min( max_lsteal, stolen );
        }
        else {
            max_lsteal = 1;
        }
        if( rsteal>0 ){
            int stolen = (int) (dampen*rsteal);
            aimFirstNoColumn -= Math.min( stolen, max_rsteal );
            if( aimFirstColumn+minLoad>aimFirstNoColumn ){
                aimFirstNoColumn = aimFirstColumn+minLoad;
            }
            max_rsteal = 1+Math.min( max_rsteal, stolen );
        }
        else {
            max_rsteal = 1;
        }
    }

    public static void main( String [] args )
    {
        int count = DEFAULTGENERATIONS;
        boolean collectStatistics = false;

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
            else if( args[i].equals( "-stats" ) ){
                collectStatistics = true;
            }
            else {
                count = Integer.parseInt( args[i] );
            }
        }
        if( collectStatistics ){
            population = new int[count];
            members = new int[count];
            sentToLeft = new int[count+1];
            sentToRight = new int[count+1];
            requestedByLeft = new int[count+1];
            requestedByRight = new int[count+1];
            computationTime = new long[count];
            communicationTime = new long[count];
            administrationTime = new long[count];
        }

        try {
            long startTime = System.currentTimeMillis();

            IbisCapabilities s = new IbisCapabilities(
                    IbisCapabilities.REGISTRY_UPCALLS,
                    IbisCapabilities.WORLDMODEL_OPEN);
            
            PortType updatePort = new PortType(
                    PortType.SERIALIZATION_DATA,
                    PortType.CONNECTION_ONE_TO_ONE,
                    PortType.COMMUNICATION_RELIABLE,
                    PortType.RECEIVE_EXPLICIT);
            
            PortType stealPort = new PortType(
                    PortType.SERIALIZATION_DATA,
                    PortType.CONNECTION_ONE_TO_ONE,
                    PortType.COMMUNICATION_RELIABLE,
                    PortType.RECEIVE_AUTO_UPCALLS);
            
            ibis = IbisFactory.createIbis( s, null, rszHandler, stealPort, updatePort );
            myName = ibis.identifier();

            registry = ibis.registry();

            ibis.enableRegistryEvents();

            if( me != 0 && leftNeighbour == null ){
                System.out.println( "P" + me + ": there is no left neighbour???" );
            }
            if( leftNeighbour != null ){
                leftReceivePort = createNeighbourReceivePort( updatePort, "upstream", null );
                leftSendPort = createNeighbourSendPort( updatePort, leftNeighbour, "downstream" );
                if( doWorkStealing ){
                    leftStealReceivePort = createNeighbourReceivePort( stealPort, "upstreamSteal", leftRecorder );
                    leftStealSendPort = createNeighbourSendPort( stealPort, leftNeighbour, "downstreamSteal" );
                }
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
                System.out.println( "Using " + ibis.getClass().getName() );
                System.out.println( "DISTURBANCE=" + DISTURBANCE );
                System.out.println( "Started a run of " + count + " generations on a " + boardsize + "x" + boardsize + " board" );
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
                receiveFromLeft( leftReceivePort, p );
            }

            while( generation<count ){
                if( false && DISTURBANCE>0 && me == 2 && generation>=DISTURBANCE_START ){
                    Thread.sleep( DISTURBANCE );
                }
                long startLoopTime = System.currentTimeMillis();
                if( rightNeighbour != null && rightReceivePort == null ){
                    // We now have a right neightbour. Set up communication
                    // with it.
                    if( tracePortCreation ){
                        System.out.println( "P" + me + ": a right neighbour has appeared; creating ports" );
                    }
                    rightReceivePort = createNeighbourReceivePort( updatePort, "downstream", null );
                    rightSendPort = createNeighbourSendPort( updatePort, rightNeighbour, "upstream" );
                    if( doWorkStealing ){
                        rightStealReceivePort = createNeighbourReceivePort( stealPort, "downstreamSteal", rightRecorder );
                        rightStealSendPort = createNeighbourSendPort( stealPort, rightNeighbour, "upstreamSteal" );
                    }
                }
                if( members != null ){
                    members[generation] = rszHandler.getMemberCount();
                }
                updateMembership( p );
                if( rightNeighbourIdle && rightSendPort != null && aimFirstNoColumn<p.firstNoColumn ){
                    // We have some work for our lazy right neighbour.
                    // give him the good news.
                    if( traceLoadBalancing ){
                        System.out.println( "P" + me + ":" + generation + ": sending work to idle neighbour P" + (me+1) );
                    }
                    sendToRight( rightSendPort, p, aimFirstColumn, aimFirstNoColumn );
                    rightNeighbourIdle = false;
                }
                if( population != null ){
                    population[generation] = p.firstNoColumn-p.firstColumn;
                }
                if( doWorkStealing ){
                    leftRecorder.reset();
                    rightRecorder.reset();
                }
                long startComputeTime = System.currentTimeMillis();
                computeNextGeneration( p );
                long endComputeTime = System.currentTimeMillis();
                if( doWorkStealing ){
                    sendStealRequest( leftStealSendPort, generation );
                    sendStealRequest( rightStealSendPort, generation );
                }
                generation++;
                updateMembership( p );
                if( doWorkStealing ){
                    lockedGeneration.set( generation );
                    int lsteal = leftRecorder.get();
                    int rsteal = rightRecorder.get();
                    if( requestedByLeft != null ){
                        requestedByLeft[generation] = lsteal;
                        requestedByRight[generation] = rsteal;
                    }
                    evaluateStealRequests( p, lsteal, rsteal );
                }
                if( (me % 2) == 0 ){
                    if( !rightNeighbourIdle ){
                        sendToRight( rightSendPort, p, aimFirstColumn, aimFirstNoColumn );
                        receiveFromRight( rightReceivePort, p );
                    }
                    sendToLeft( leftSendPort, p, aimFirstColumn, aimFirstNoColumn );
                    receiveFromLeft( leftReceivePort, p );
                }
                else {
                    receiveFromLeft( leftReceivePort, p );
                    sendToLeft( leftSendPort, p, aimFirstColumn, aimFirstNoColumn );
                    if( !rightNeighbourIdle ){
                        receiveFromRight( rightReceivePort, p );
                        sendToRight( rightSendPort, p, aimFirstColumn, aimFirstNoColumn );
                    }
                }
                long endTime = System.currentTimeMillis();
                if( computationTime != null ){
                    computationTime[generation-1] = (endComputeTime-startComputeTime);
                    communicationTime[generation-1] = (endTime-endComputeTime);
                    administrationTime[generation-1] = (startComputeTime-startLoopTime);
                }
                if( showProgress && me == 0 ){
                    System.out.print( '.' );
                }
            }
            if( showProgress && me == 0 ){
                System.out.println();
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
            if( population != null ){
                // We blindly assume all statistics arrays exist.
                for( int gen=0; gen<count; gen++ ){
                    System.out.println( "STATS " + me + " " + gen + " " + members[gen] + " " + population[gen] + " " + sentToLeft[gen] + " " + sentToRight[gen] + " " + computationTime[gen] + " " + communicationTime[gen] + " " + administrationTime[gen] + " " + requestedByLeft[gen] + " " + requestedByRight[gen] );
                }
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
