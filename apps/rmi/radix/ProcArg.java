
import java.io.Serializable;

class ProcArg implements Serializable {

    static final int DEFAULT_RADIX = 1024;

    static final int DEFAULT_LOG2 = 10;

    int radix, log2_Radix, num_Keys;

    int[] key_Partition;

    ProcArg() {
        radix = DEFAULT_RADIX;
        log2_Radix = DEFAULT_LOG2;
        num_Keys = 0;
    }

    ProcArg(int num_Keys, int radix, int log2, int[] key_Partition) {
        this.num_Keys = num_Keys;
        this.radix = radix;
        log2_Radix = log2;
        this.key_Partition = key_Partition;

    }

    ProcArg(ProcArg proc) {
        this.radix = proc.radix;
        this.log2_Radix = proc.log2_Radix;
        this.num_Keys = proc.num_Keys;
        key_Partition = new int[proc.key_Partition.length];
        for (int i = 0; i < proc.key_Partition.length; i++) {
            this.key_Partition[i] = proc.key_Partition[i];
        }
    }

}