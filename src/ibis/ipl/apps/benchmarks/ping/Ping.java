package ibis.ipl.apps.benchmarks.ping;

/* $Id$ */

import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.Registry;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;
import ibis.util.Timer;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * One-way and two-way benchmarking application.
 *
 * Average results are displayed through STDOUT. Individual results are displayed through STDERR.
 * The fields of the 'average results' line are:
 * <OL>
 * <LI> the 'ping' processus rank (0 or 1);
 * <LI> the 'pong' processus rank (1 or 0);
 * <LI> the message size;
 * <LI> the bandwidth expressed in 10^6 bytes;
 * <LI> the bandwidth expressed in 1024^2 bytes;
 * <LI> the latency in microseconds.
 * </OL>
 * The 'individual results' line also display the test cycle number. The test cycle numbered 0 is a warmup test cycle and is ignored when computing the average results.
 */
public final class Ping {

    /**
     * Flag indicating whether the application should perform one-way
     * benchmarking or two-way benchmarking.
     */
    final static boolean param_one_way = false;

    /**
     * Flag activating data transfer checking.
     *
     * When this flag is set, a specific data pattern is used to mark to buffer before sending.
     * Once received, the actual buffer content is checked against the expected pattern
     * to detect missing or incorrect data.
     * <BR><B>Note</B>: this flag should of course not be set while benchmarking, to avoid
     * performance biasing!
     */
    final static boolean param_control_receive = false;

    /**
     * Parameter indicating how many measured testing cycles
     * should be performed for each message size.
     *
     * The {@link #param_nb_tests} individual results are sent over STDERR.
     * The average of {@link #param_nb_tests} individual results is sent over STDOUT.
     *
     * <BR><B>Note</B>: an additional dummy testing cycle is performed as a warm-up before the
     * {@link #param_nb_tests} testing cycles.
     */
    final static int param_nb_tests = 5;

    /**
     * Parameter indicating the number of sample iteration within one testing cycle.
     */
    final static int param_nb_samples = 1000;

    /**
     * The minimum message size.
     */
    final static int param_min_size = 1;

    /**
     * The maximum message size.
     */
    final static int param_max_size = 2 * 1024 * 1024;

    /**
     * The increasing message size step.
     * If {@link #param_step} value is <code>0</code> the message size is doubled
     * on each iteration.
     */
    final static int param_step = 0;

    /**
     * Flag indicating whether empty messages should automatically be replaced
     * by one-byte messages
     */
    final static boolean param_no_zero = false;

    /**
     * Flag indicating whether the buffer should be filled before sending.
     */
    final static boolean param_fill_buffer = true;

    /**
     * The value to use for filling the buffer if {@link #param_fill_buffer} is set.
     * <BR><B>Note</B>: if the {@link #param_control_receive} flag is set,
     * the buffer is actually filled using a specific data pattern.
     */
    final static byte param_fill_buffer_value = 1;

    /**
     * Activate some debugging logs.
     */
    final static boolean param_log = false;

    /*
     * ____________________________________________________________________
     * Internal variables
     *
     */
    final static Timer timer = Timer.createTimer();

    static byte[] buffer = null;

    static int rank = 0;

    static int remoteRank = 1;

    static SendPort sport = null;

    static ReceivePort rport = null;

    static Ibis ibis = null;

    static Registry registry = null;

    static void mark_buffer(int len) {
        byte n = 0;
        int i = 0;

        for (i = 0; i < len; i++) {
            n += 7;
            buffer[i] = n;
        }
    }

    static void clear_buffer() {
        int i = 0;

        for (i = 0; i < buffer.length; i++) {
            buffer[i] = 0;
        }
    }

    static void control_buffer(int len) {
        boolean ok = true;
        byte n = 0;
        int i = 0;

        for (i = 0; i < len; i++) {
            n += 7;

            if (buffer[i] != n) {
                byte b1 = 0;
                byte b2 = 0;

                b1 = n;
                b2 = buffer[i];
                System.out.println("Bad data at byte " + i + ": expected " + b1
                        + ", received " + b2);
                ok = false;
            }
        }

        if (!ok) {
            System.out.println(len + " bytes reception failed");
        }
    }

    static void fill_buffer() {
        int i = 0;
        for (i = 0; i < buffer.length; i++) {
            buffer[i] = param_fill_buffer_value;
        }
    }

    static String alignRight(double num, int fieldSize) {
        StringBuffer fb = new StringBuffer();

        for (int i = 0; i < fieldSize; i++) {
            fb.append(' ');
        }

        NumberFormat f = NumberFormat.getNumberInstance();

        if (f instanceof DecimalFormat) {
            ((DecimalFormat) f).applyPattern("#0.00");
        }

        fb.append(f.format(num));
        int l = fb.length();

        return fb.substring(l - fieldSize, l);
    }

    static String alignRight(int num, int fieldSize) {
        StringBuffer fb = new StringBuffer();

        for (int i = 0; i < fieldSize; i++) {
            fb.append(' ');
        }

        NumberFormat f = NumberFormat.getNumberInstance();

        if (f instanceof DecimalFormat) {
            ((DecimalFormat) f).applyPattern("#0");
        }

        fb.append(f.format(num));
        int l = fb.length();

        return fb.substring(l - fieldSize, l);
    }

    static void dispTestResult(int test, int _size, double result) {
        double millionbyte = (_size * (double) param_nb_samples)
                / (result / (2 - (param_one_way ? 1 : 0)));
        double megabyte = millionbyte / 1.048576;
        double latency = result / param_nb_samples
                / (2 - (param_one_way ? 1 : 0));

        StringBuffer fb = param_one_way ? new StringBuffer("* test " + test
                + ": " + rank + "->" + remoteRank + "  ") : new StringBuffer(
                "* test " + test + ": ");
        fb.append(alignRight(_size, 7));
        fb.append("  ");
        fb.append(alignRight(millionbyte, 15));
        fb.append("  ");
        fb.append(alignRight(megabyte, 15));
        fb.append("  ");
        fb.append(alignRight(latency, 15));
        System.err.println("" + fb);
    }

    static void dispAverageResult(int _size, double result) {
        double millionbyte = (_size * (double) param_nb_tests * (double) param_nb_samples)
                / (result / (2 - (param_one_way ? 1 : 0)));
        double megabyte = millionbyte / 1.048576;
        double latency = result / param_nb_tests / param_nb_samples
                / (2 - (param_one_way ? 1 : 0));
        StringBuffer fb = param_one_way ? new StringBuffer("= " + rank + "->"
                + remoteRank + "  ") : new StringBuffer("= ");

        fb.append(alignRight(_size, 7));
        fb.append("  ");
        fb.append(alignRight(millionbyte, 15));
        fb.append("  ");
        fb.append(alignRight(megabyte, 15));
        fb.append("  ");
        fb.append(alignRight(latency, 15));
        System.out.println("" + fb);
    }

    static double oneWayPing(final int _size) throws Exception {
        double sum = 0.0;
        int nb_tests = param_nb_tests + 1;

        while ((nb_tests--) > 0) {
            int nb_samples = param_nb_samples;

            timer.reset();
            timer.start();
            if (param_control_receive) {
                mark_buffer(_size);
            }

            while ((nb_samples--) > 0) {
                if (param_log) {
                    System.err.println("Ping: sending: --> " + nb_samples);
                }

                WriteMessage out = sport.newMessage();
                out.writeArray(buffer);

                out.finish();
                if (param_log) {
                    System.err.println("Ping: sending: <-- " + nb_samples);
                }
            }

            if (param_control_receive) {
                clear_buffer();
            }

            if (param_log) {
                System.err.println("Ping: receiving: -->");
            }

            ReadMessage in = rport.receive();
            in.readArray(buffer);
            in.finish();

            if (param_control_receive) {
                control_buffer(_size);
            }

            if (param_log) {
                System.err.println("Ping: receiving: <--");
            }

            timer.stop();
            double _time = timer.totalTimeVal();

            dispTestResult(param_nb_tests - nb_tests, _size, _time);

            if ((param_nb_tests - nb_tests) > 0) {
                sum += _time;
            }
        }

        return sum;
    }

    static double twoWayPing(final int _size) throws Exception {
        double sum = 0.0;
        int nb_tests = param_nb_tests + 1;

        while ((nb_tests--) > 0) {
            int nb_samples = param_nb_samples;

            timer.reset();
            timer.start();
            while ((nb_samples--) > 0) {
                if (param_control_receive) {
                    mark_buffer(_size);
                }

                if (param_log) {
                    System.err.println("Ping: sending: --> " + nb_samples);
                }

                WriteMessage out = sport.newMessage();
                out.writeArray(buffer);
                out.finish();
                if (param_log) {
                    System.err.println("Ping: sending: <-- " + nb_samples);
                }

                if (param_control_receive) {
                    clear_buffer();
                }

                if (param_log) {
                    System.err.println("Ping: receiving: --> " + nb_samples);
                }

                ReadMessage in = rport.receive();
                in.readArray(buffer);
                in.finish();
                if (param_log) {
                    System.err.println("Ping: receiving: <-- " + nb_samples);
                }

                if (param_control_receive) {
                    control_buffer(_size);
                }
            }
            timer.stop();

            double _time = timer.totalTimeVal();
            dispTestResult(param_nb_tests - nb_tests, _size, _time);

            if ((param_nb_tests - nb_tests) > 0) {
                sum += _time;
            }
        }

        return sum;
    }

    static void oneWayPong(final int _size) throws Exception {
        int nb_tests = param_nb_tests + 1;

        while ((nb_tests--) > 0) {
            int nb_samples = param_nb_samples;
            ;

            while ((nb_samples--) > 0) {
                if (param_log) {
                    System.err.println("Pong: receiving: --> " + nb_samples);
                }

                ReadMessage in = rport.receive();
                in.readArray(buffer);
                in.finish();
                if (param_log) {
                    System.err.println("Pong: receiving: <-- " + nb_samples);
                }

            }

            if (param_log) {
                System.err.println("Pong: sending: -->");
            }
            WriteMessage out = sport.newMessage();
            out.writeArray(buffer);
            out.finish();
            if (param_log) {
                System.err.println("Pong: sending: <--");
            }
        }
    }

    static void twoWayPong(final int _size) throws Exception {
        int nb_tests = param_nb_tests + 1;

        while ((nb_tests--) > 0) {
            int nb_samples = param_nb_samples;
            ;

            while ((nb_samples--) > 0) {
                if (param_log) {
                    System.err.println("Pong: receiving: --> " + nb_samples);
                }

                ReadMessage in = rport.receive();
                in.readArray(buffer);
                in.finish();
                if (param_log) {
                    System.err.println("Pong: receiving: <-- " + nb_samples);
                }

                if (param_log) {
                    System.err.println("Pong: sending: --> " + nb_samples);
                }

                WriteMessage out = sport.newMessage();
                out.writeArray(buffer);
                out.finish();
                if (param_log) {
                    System.err.println("Pong: sending: <-- " + nb_samples);
                }
            }
        }
    }

    static void ping() throws Exception {
        int size = 0;
        for (size = param_min_size; size <= param_max_size; size = (param_step != 0) ? size
                + param_step
                : size * 2) {
            final int _size = (size == 0 && param_no_zero) ? 1 : size;
            double result = 0;

            buffer = new byte[_size];
            if (param_fill_buffer) {
                fill_buffer();
            }

            System.err.println("\n--------------------");
            System.err.println("size = " + _size + "\n");

            if (param_one_way) {
                result = oneWayPing(_size);
            } else {
                result = twoWayPing(_size);
            }

            System.err.println();
            dispAverageResult(_size, result);

            buffer = null;
        }
    }

    static void pong() throws Exception {
        int size = 0;

        for (size = param_min_size; size <= param_max_size; size = (param_step != 0) ? size
                + param_step
                : size * 2) {
            final int _size = (size == 0 && param_no_zero) ? 1 : size;

            buffer = new byte[_size];

            if (param_one_way) {
                oneWayPong(_size);
            } else {
                twoWayPong(_size);
            }

            buffer = null;
        }
    }

    static void master() throws Exception {
        if (param_one_way) {
            ping();
            pong();
        } else {
            ping();
        }
    }

    static void slave() throws Exception {
        if (param_one_way) {
            pong();
            ping();
        } else {
            pong();
        }
    }

    public static void main(String[] args) {
        try {
            // Local initialization

            IbisCapabilities props = new IbisCapabilities(
                    IbisCapabilities.WORLDMODEL_CLOSED);
            
            PortType t = new PortType(
                    PortType.SERIALIZATION_BYTE,
                    PortType.COMMUNICATION_RELIABLE,
                    PortType.CONNECTION_ONE_TO_ONE,
                    PortType.RECEIVE_EXPLICIT);
            
            ibis = IbisFactory.createIbis(props, null, null, t);

            // Configuration information
            registry = ibis.registry();
            IbisIdentifier master = registry.elect("ping");

            if (master.equals(ibis.identifier())) {
                rank = 0;
                remoteRank = 1;
            } else {
                rank = 1;
                remoteRank = 0;
            }

            // Local communication setup

            sport = ibis.createSendPort(t);
            rport = null;
            ibis.registry().elect("" + rank);

            // Connection setup and test
            if (rank == 0) {
                rport = ibis.createReceivePort(t, "ping");
                rport.enableConnections();
                IbisIdentifier id = ibis.registry().getElectionResult("1");
                sport.connect(id, "ping");

                System.err.println("Master: starting test");
                master();
            } else {
                IbisIdentifier id = ibis.registry().getElectionResult("0");
                sport.connect(id, "ping");
                rport = ibis.createReceivePort(t, "ping");
                rport.enableConnections();

                System.err.println("Slave: starting test");
                slave();
            }
            sport.close();
            rport.close();
            ibis.end();
        } catch (Exception e) {
            System.out.println("Got exception " + e);
            System.out.println("StackTrace:");
            e.printStackTrace();
        }
    }
}
