/* File: $Id$
 *
 * Successive over relaxation
 * SUN RMI version implementing a red-black SOR, based on earlier Orca source.
 * with cluster optimization, and split phase optimization, reusing a thread
 * each Wide-Area send. ( All switchable )
 *
 * Rob van Nieuwpoort & Jason Maassen
 *
 */

import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.*;

class Main {
    static PoolInfoClient info = SOR.info;

    private static void usage( String[] args ) {
        if( info.rank() == 0 ) {
            System.out.println( "Usage: sor [options] <NROW> <NCOL> <ITERATIONS>" );
            System.out.println( "" );
            System.out.println( "NROW NCOL   : ( int, int ). Problem matrix size" );
            System.out.println( "ITERATIONS    : ( int ). Number of iterations to calculate. 0 means dynamic termination detection." );
            System.out.println( "options:" );
            System.out.println( "-async: use split phase optimization and asynchronous sends." );
            System.out.println( "\noptions you gave: " );

            for( int i=0;i<args.length;i++ ) {
                System.out.println( i + " : " + args[i] );
            }
            System.exit( 1 );
        }
    }

    public static void main ( String[] args )
    {
        SOR local = null;
        SORInterface [] table = null;
        GlobalDataInterface global = null;
        Registry reg = null;

        /* set up problem size */
        int nrow = 1000, ncol = 1000, nit = 50;
        boolean sync = true;
        int optionCount = 0;

        for( int i=0; i<args.length; i++ ){
            if( args[i].equals( "-async" ) ) {
                sync = false;
            }
            else {
                if( optionCount == 0 ) {
                    try {
                        nrow = Integer.parseInt( args[i] );
                    } catch ( Exception e ) {
                        usage( args );
                    }
                    optionCount++;
                }
                else if( optionCount == 1 ) {
                    try {
                        ncol = Integer.parseInt( args[i] );
                    }
                    catch ( Exception e ) {
                        usage( args );
                    }
                    optionCount++;
                }
                else if( optionCount == 2 ) {
                    try {
                        nit = Integer.parseInt( args[i] );
                    }
                    catch ( Exception e ) {
                        usage( args );
                    }
                    optionCount++;
                }
                else {
                    usage( args );
                }
            }
        }

        if(  nrow < info.size() ) {
            /* give each process at least one row */
            if( info.rank() == 0 ) {
                System.out.println( "Problem to small for number of CPU's" );
            }
            System.exit( 1 );
        }

        try {
                // Start the registry.
                reg = RMI_init.getRegistry( info.hostName( 0 ) );

                if( info.rank() == 0 ) {
                    System.out.println( "Starting SOR" );
                    System.out.println( "" );
                    System.out.println( "CPUs          : " + info.size() );
                    System.out.println( "Matrix size   : " + nrow + "x" + ncol );
                    System.out.println( "Iterations    : " + ( ( nit == 0 ) ? "dynamic" : ( "" + nit ) ) );
                    System.out.println( "Calculation   : " + ( ( sync == SOR.SYNC_SEND ) ? "one-phase"   : "split-phase" ) );

                    System.out.println();

                    global = new GlobalData( info, true );
                    Naming.bind( "GlobalData", global );
                    System.err.println( "I am the master: " + info.hostName( 0 ) );
                }
                else {
                    int i = 0;
                    boolean done = false;

                    while( !done && i < 10 ){
                        i++;
                        done = true;

                        try {
                            global = (GlobalDataInterface) Naming.lookup( "//" + info.hostName( 0 ) + "/GlobalData" );
                        }
                        catch ( Exception e ) {
                            done = false;
                            Thread.sleep( 2000 );
                        }
                    }

                    if( !done ) {
                        System.out.println( info.rank() + " could not connect to " + "//" + info.hostName( 0 ) + "/GlobalData" );
                        System.exit( 1 );
                    }
                }

                local = new SOR( nrow, ncol, nit, sync, global );

                table = global.table( (SORInterface) local, info.rank() );
                local.setTable( table );

                local.start();

                if( info.rank() == 0 ) {
                    Thread.sleep( 2000 );
                    // Give the other nodes a chance to exit.
                    // before cleaning up the GlobalData object.
                }
                System.exit( 0 );
        }
        catch ( Exception e ) {
            System.err.println( "Uncaught " + e );
            e.printStackTrace();
        }
    }
}
