/* $Id$ */


import java.io.*;

class Main {

    public static void main(String[] args) {

        try {
            int len = Integer.parseInt(args[0]);
            int count = Integer.parseInt(args[1]);

            Tree t = new Tree(len);
            int[] a = new int[1024];
            int index = 0;

            long start = System.currentTimeMillis();

            for (int i = 0; i < count; i++) {
                t.generated_WriteObject(a, index);
            }

            long end = System.currentTimeMillis();

            long time = end - start;
            double per = ((double) time) / count;

            System.out.println("Single tree traversed in " + per + " ms.");

            double maxtp = (((len * 40) * count) / (1024.0 * 1024.0))
                    / (time / 1000.0);

            System.out.println("Max Karmi TP = " + maxtp);

        } catch (Exception e) {
            System.out.println("OOPS" + e);
            e.printStackTrace();
        }
    }
}