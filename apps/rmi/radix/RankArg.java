
import java.io.Serializable;

class RankArg implements Serializable {

    static final int DEFAULT_RADIX = 1024;

    static final int DEFAULT_SHIFT = 10;

    static final int DEFAULT_MASK = 1024;

    int nr_Keys, radix, shift, mask;

    RankArg() {
        nr_Keys = 0;
        radix = DEFAULT_RADIX;
        shift = DEFAULT_SHIFT;
        mask = DEFAULT_MASK;
    }

    RankArg(int nr_Keys) {
        this.nr_Keys = nr_Keys;
        radix = DEFAULT_RADIX;
        shift = DEFAULT_SHIFT;
        mask = DEFAULT_MASK;
    }

    RankArg(int nr_Keys, int radix, int shift, int mask) {
        this.nr_Keys = nr_Keys;
        this.radix = radix;
        this.shift = shift;
        this.mask = mask;
    }

}