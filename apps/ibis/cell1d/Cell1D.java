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
        for ( int r=0; r<repeat; r++ ) {
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
            for ( int r=0; r<repeat; r++ ) {
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
        System.out.println( "Usage: Cell1D [-u] [-uu] [-ibis] [count]" );
        System.exit( 0 );
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
            info = new ibis.util.PoolInfo();
            StaticProperties s = new StaticProperties();
            s.add( "serialization", "data" );
            s.add( "communication", "OneToOne, Reliable, ExplicitReceipt" );
            s.add( "worldmodel", "open" );
            ibis = Ibis.createIbis( s, rszHandler );

            ibis.openWorld();

            registry = ibis.registry();

            PortType t = ibis.createPortType( "cell type", s );

            SendPort sport = t.createSendPort( "send port" );
            ReceivePort rport;
            Cell1D lat = null;

            if( DEBUG ) {
                System.out.println( "LAT: pre elect" );
            }
            System.err.println( "Node " + info.rank() + "/" + info.size() + " present" );
            IbisIdentifier master = ( IbisIdentifier ) registry.elect( "latency", ibis.identifier() );
            if( DEBUG ) {
                System.out.println( "LAT: post elect" );
            }

            if( master.equals( ibis.identifier() ) ) {
                if( DEBUG ) {
                    System.out.println( "LAT: I am master" );
                }
                rank = 0;
                remoteRank = 1;
            }
            else {
                if( DEBUG ) {
                    System.out.println( "LAT: I am slave" );
                }
                rank = 1;
                remoteRank = 0;
            }

            if( rank == 0 ) {
                rport = t.createReceivePort( "test port 0" );
                rport.enableConnections();
                ReceivePortIdentifier ident = lookup( "test port 1" );
                connect( sport, ident );
                Sender sender = new Sender( rport, sport );

                if( DEBUG ) {
                    System.out.println( "LAT: starting send test" );
                }
                sender.send( count, repeat );
            }
            else {
                ReceivePortIdentifier ident = lookup( "test port 0" );
                connect( sport, ident );

                rport = t.createReceivePort( "test port 1" );
                rport.enableConnections();

                Receiver receiver = new Receiver( rport, sport );
                if( DEBUG ) {
                    System.out.println( "LAT: starting test receiver" );
                }
                receiver.receive( count, repeat );
            }

            /* free the send ports first */
            sport.close();
            rport.close();
            ibis.end();
        }
        catch( Exception e ) {
            System.err.println( "Got exception " + e );
            System.err.println( "StackTrace:" );
            e.printStackTrace();
        }
    }
}
