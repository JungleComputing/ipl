/* $Id$ */


import java.io.Serializable;

public class Data implements Serializable {

    double value;

    Data next;

    public Data() {
    }

    public Data(double value, Data next) {
        this.value = value;
        this.next = next;
    }
}