// File: $Id$

// A replacement step, represented as a list of text positions and a length.
class Step implements java.io.Serializable {
    int occurences[];

    int len;

    public Step(int occ[], int n, int l) {
        occurences = new int[n];

        System.arraycopy(occ, 0, occurences, 0, n);
        len = l;
    }

    public String toString() {
        String res = "([";

        for (int i = 0; i < occurences.length; i++) {
            if (i != 0) {
                res += ",";
            }
            res += occurences[i];
        }
        return res + "],len=" + len + ",gain=" + getGain() + ")";
    }

    public int getGain() {
        int gain = occurences.length * (len - 1);
        int loss = len + 1;
        return gain - loss;
    }
}