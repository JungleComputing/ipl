// File: $Id$

// A replacement step, represented as two text positions and a length.
class Step {
    int i;
    int j;
    int len;

    public Step( int a, int b, int l )
    {
        i = a;
        j = b;
        len = l;
    }

    public String toString()
    {
        return "(" + i + "," + j + ",len=" + len + ")";
    }
}
