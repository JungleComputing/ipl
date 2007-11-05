package ibis.ipl.benchmarks.rpc;

/* $Id: Data1.java 6546 2007-10-05 13:21:40Z ceriel $ */



final class Data1 implements java.io.Serializable {

    private static final long serialVersionUID = 7366807900682527528L;

    static int fill;

    int i0;

    int i1;

    int i2;

    int i3;

    Data1() {
        i0 = fill++;
        i1 = fill++;
        i2 = fill++;
        i3 = fill++;
    }

}