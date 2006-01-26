/* $Id$ */

import ibis.io.DataInputStream;
import ibis.io.DataOutputStream;
import ibis.io.IbisSerializationInputStream;
import ibis.io.IbisSerializationOutputStream;
import ibis.io.SerializationInput;
import ibis.io.SerializationOutput;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class StreamTest {
    private static final int MLOOPSIZE = 25;

    private static final int SMAX = 2000000; // 5000000;

    private static final int SMIN = 4;

    private static final boolean VERIFY = false;

    private static final boolean TEST_SUN = false;

    public static void main(String args[]) {

        int m_size = 0;
        double logsize = Math.log((double) SMAX) - Math.log((double) SMIN);

        /* loop over no of different message sizes */
        for (int l = 0; l < MLOOPSIZE; l++) {
            
            System.gc();
            
            m_size = (int) (Math.exp(Math.log((double) SMIN)
                    + (double) ((double) l / (double) MLOOPSIZE * logsize)));
            obj_double[] inArray = new obj_double[m_size];
            obj_double[] outArray = new obj_double[m_size];
            for (int k = 0; k < m_size; k++) {
                inArray[k] = new obj_double(k);
            }

            System.out.println("Array size: " + m_size);
            if (TEST_SUN) {
                try {
                    long start = System.currentTimeMillis();
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    ObjectOutputStream oout = new ObjectOutputStream(bout);
                    oout.writeObject(inArray);
                    oout.flush();
                    ByteArrayInputStream bin = new ByteArrayInputStream(bout
                            .toByteArray());
                    ObjectInputStream oin = new ObjectInputStream(bin);
                    outArray = (obj_double[]) oin.readObject();
                    long time = System.currentTimeMillis() - start;
                    double d = time / 1000.0;

                    if (VERIFY) {
                        for (int i = 0; i < m_size; i++) {
                            if (outArray[i].x != i) {
                                throw new Error("verify error");
                            }
                        }
                    }
                    System.err.println("sun took: " + d + " s");
                    System.gc();
                } catch (Exception e) {
                    throw new Error(e);
                }
            }

            StoreBuffer stBuf = new StoreBuffer();

            try {
                DataOutputStream out = null;
                DataInputStream in = null;
                SerializationOutput mout = null;
                SerializationInput min = null;

                out = new StoreArrayOutputStream(stBuf);
                in = new StoreArrayInputStream(stBuf);
                mout = new IbisSerializationOutputStream(out);
                min = new IbisSerializationInputStream(in);

                long refWriTime = System.currentTimeMillis();
                mout.writeArray(((Object[]) inArray), 0, m_size);
                long finWriTime = System.currentTimeMillis();

                mout.flush();
                long finFluTime = System.currentTimeMillis();

                min.readArray((Object[]) outArray, 0, m_size);
                long finReaTime = System.currentTimeMillis();

                if (VERIFY) {
                    for (int i = 0; i < m_size; i++) {
                        if (outArray[i].x != i) {
                            throw new Error("verify error, val = "
                                    + outArray[i].x + " should be " + i);
                        }
                    }
                }
                System.out
                        .println("write time: "
                                + (double) (((double) (finWriTime - refWriTime)) / 1000)
                                + " sec");
                System.out
                        .println("flush time: "
                                + (double) (((double) (finFluTime - finWriTime)) / 1000)
                                + " sec");
                System.out
                        .println("read time: "
                                + (double) (((double) (finReaTime - finFluTime)) / 1000)
                                + " sec");
                System.out
                        .println("complete: "
                                + (double) (((double) (finReaTime - refWriTime)) / 1000)
                                + " sec");
                System.out.println();

                mout.realClose();
                min.realClose();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }

        }
    }

}
