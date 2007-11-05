package ibis.ipl.benchmarks.masterWorker;

/* $Id: Data.java 6546 2007-10-05 13:21:40Z ceriel $ */


import java.io.Serializable;

public final class Data implements Serializable {
    private static final long serialVersionUID = 3320470528572444820L;

    boolean bool = true;

    byte b = 5;

    char c = 'a';

    short s = -32000;

    int i = 1000 * 1000;

    long l = 405949349;

    float f = (float) -13.4;

    double d = 3.454334783478437834;

    //    int[] array = new int[32000];

    Data() {
        //	Random random = new Random();
        //	for (int i = 0; i < array.length; i++) {
        //	    array[i] = random.nextInt();
        //	}
    }

    public boolean equals(Object other) {
        if (!(other instanceof Data)) {
            return false;
        }

        return equals((Data) other);
    }

    public boolean equals(Data other) {
        //	for (int i = 0; i < array.length; i++) {
        //	    if (array[i] != other.array[i]) {
        //		return false;
        //	    }
        //	}
        //

        return (other.bool = bool && other.b == b && other.c == c
                && other.s == s && other.i == i && other.l == l && other.f == f
                && other.d == d);
    }
}

