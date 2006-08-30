/* $Id$ */


import java.io.Serializable;
import java.io.IOException;

public final class Data implements Serializable {

    double v1, v2, v3, v4;

    Data next;

    public Data(double value, Data next) {
        v1 = v2 = v3 = v4 = value;
        this.next = next;
    }
}