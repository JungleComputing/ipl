/* $Id$ */


import java.io.Serializable;

final class Data1 implements java.io.Serializable {

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