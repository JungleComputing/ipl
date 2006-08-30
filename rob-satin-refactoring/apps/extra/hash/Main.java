/* $Id$ */


import ibis.io.*;
import java.io.*;
import java.util.*;

class Main {

    public static void main(String[] args) {

        try {
            int count = Integer.parseInt(args[0]);

            HashMap h = new HashMap();

            for (int j = 16; j < (64 * 1024); j *= 2) {

                Tree t = new Tree(j);

                long start = System.currentTimeMillis();

                for (int i = 0; i < count; i++) {
                    t.generated_WriteObject(h);
                    h.clear();
                }

                long end = System.currentTimeMillis();

                long time = end - start;
                double per = ((double) time) / count;

                //System.out.println("Single tree traversed in " + per + " ms.");

                double maxtp = (((j * 40) * count) / (1024.0 * 1024.0))
                        / (time / 1000.0);

                //System.out.println("Max Karmi TP = " + maxtp);
                System.out.println(j + " " + maxtp);
            }

        } catch (Exception e) {
            System.out.println("OOPS" + e);
            e.printStackTrace();
        }
    }
}