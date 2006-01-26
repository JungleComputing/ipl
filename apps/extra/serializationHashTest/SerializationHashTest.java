/* $Id$ */

import ibis.io.HandleHash;

import java.io.Serializable;
import java.util.Random;

/*
 * Created on Aug 11, 2005 by rob
 */
class D implements Serializable {
    double d;
}

public class SerializationHashTest {

    static final int MAX_SIZE = 1024 * 1024;

    static final int REPEATS = 20;

    public static void main(String[] args) {
        D[] objects = new D[MAX_SIZE];
        int[] hashcodes = new int[MAX_SIZE];
        for (int i = 0; i < MAX_SIZE; i++) {
            objects[i] = new D();
            hashcodes[i] = System.identityHashCode(objects[i]);
        }
        HandleHash hash = new HandleHash();

        for (int r = 0; r < 20; r++) {
            long start = System.currentTimeMillis();
            for (int i = 0; i < MAX_SIZE; i++) {
                hash.put(objects[i], 0, hashcodes[i]);
            }
            long t = System.currentTimeMillis() - start;
            double time = (double) t / 1000.0;
            double objTime = (double) t * 1000.0 / MAX_SIZE; // us / object

            System.err.println("added " + MAX_SIZE + " objects in " + time
                    + " seconds, " + objTime + " us / object");
            if (r != REPEATS - 1)
                hash.clear();
        }

        Random rand = new Random();

        // now do random lookups
        for (int r = 0; r < 20; r++) {
            long start = System.currentTimeMillis();
            for (int i = 0; i < MAX_SIZE; i++) {
                int pos = rand.nextInt(MAX_SIZE);
                hash.find(objects[pos], hashcodes[pos]);
            }
            long t = System.currentTimeMillis() - start;
            double time = (double) t / 1000.0;
            double objTime = (double) t * 1000.0 / MAX_SIZE; // us / object
            System.err.println("lookup of " + MAX_SIZE + " objects in " + time
                    + " seconds, " + objTime + " us / object");
            if (r != REPEATS - 1)
                hash.clear();
        }
    }
}
