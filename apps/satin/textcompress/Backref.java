// File: $Id$

class Backref {
    int backpos = -1;
    int len = -1;
    int gain = 0;       // The gain of using this backref.

    public String toString()
    {
        return "backpos=" + backpos + ", len=" + len +  ", gain=" + gain;
    }
}
