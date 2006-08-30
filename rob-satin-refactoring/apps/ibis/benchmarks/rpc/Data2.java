/* $Id$ */


import java.io.Serializable;

final class Data2 implements java.io.Serializable {

    static int fill;

    int i0;

    int i1;

    int i2;

    int i3;

    Data2 left;

    Data2 right;

    Data2(int n) {
        i0 = fill++;
        i1 = fill++;
        i2 = fill++;
        i3 = fill++;

        int l = (n - 1) / 2;
        int r = n - 1 - l;
        if (l > 0) {
            left = new Data2(l);
        }
        if (r > 0) {
            right = new Data2(r);
        }
    }

}