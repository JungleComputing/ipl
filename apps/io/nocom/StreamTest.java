import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import java.util.ArrayList;

import ibis.io.DataInputStream;
import ibis.io.DataOutputStream;
import ibis.io.BufferedArrayInputStream;
import ibis.io.BufferedArrayOutputStream;
import ibis.io.IbisSerializationInputStream;
import ibis.io.IbisSerializationOutputStream;
import ibis.io.SunSerializationInputStream;
import ibis.io.SunSerializationOutputStream;
import ibis.io.SerializationInput;
import ibis.io.SerializationOutput;

public class StreamTest {
    private static final int MLOOPSIZE = 25;

    private static final int SMAX = 5000000;

    private static final int SMIN = 4;

    public static void main(String args[]) {

        int m_size = 0;
        double logsize = Math.log((double) SMAX) - Math.log((double) SMIN);

        /* loop over no of different message sizes */
        for (int l = 0; l < MLOOPSIZE; l++) {
            m_size = (int) (Math.exp(Math.log((double) SMIN)
                    + (double) ((double) l / (double) MLOOPSIZE * logsize)));
            obj_double[] inArray = new obj_double[m_size];
            obj_double[] outArray = new obj_double[m_size];
            for (int k = 0; k < m_size; k++) {
                inArray[k] = new obj_double(0.0);
            }

            try {
                long start = System.currentTimeMillis();
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                ObjectOutputStream oout = new ObjectOutputStream(bout);
                oout.writeObject(inArray);
                oout.flush();
                ByteArrayInputStream bin = new ByteArrayInputStream(bout
                        .toByteArray());
                ObjectInputStream oin = new ObjectInputStream(bin);
                oin.readObject();
                long time = System.currentTimeMillis() - start;
                double d = time / 1000.0;

                System.err.println("sun took: " + d + " s");
            } catch (Exception e) {
                throw new Error(e);
            }

            StoreBuffer stBuf = new StoreBuffer();

            try {
                DataOutputStream out = null;
                DataInputStream in = null;
                StoreArrayInputStream sin = null;
                SerializationOutput mout = null;
                SerializationInput min = null;

                out = new StoreArrayOutputStream(stBuf);
                in = new StoreArrayInputStream(stBuf);
                sin = (StoreArrayInputStream) in;
                mout = new IbisSerializationOutputStream(out);
                min = new IbisSerializationInputStream(in);

                long refWriTime = System.currentTimeMillis();
                mout.writeArray(((Object[]) inArray), 0, m_size);
                long finWriTime = System.currentTimeMillis();

                mout.flush();
                long finFluTime = System.currentTimeMillis();

                min.readArray((Object[]) outArray, 0, m_size);
                long finReaTime = System.currentTimeMillis();

                System.out.println("Array size: " + m_size);
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