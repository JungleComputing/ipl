//
// Class Leaf
//
// Quad tree stuff
//
class Leaf implements java.io.Serializable {
    private float[][] value;

    int size;

    private Leaf() {
    }

    Leaf(int task, int rec, int loop, float v, boolean flipped) {
        if (task + rec == 0) {
            this.size = loop;
            value = new float[loop][loop];
            for (int i = 0; i < loop; i++) {
                for (int j = 0; j < loop; j++) {
                    value[i][j] = v;
                }
                if (flipped) {
                    v = -v;
                }
            }
        }
    }

    public void print() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                System.out.print(value[i][j] + " ");
            }
            System.out.println();
        }
    }

    public boolean check(float result) {
        boolean ok = true;

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (value[i][j] != result) {
                    System.out.println("ERROR in matrix!, i = " + i + ", j = "
                            + j + " val = " + value[i][j]);
                    ok = false;
                }
            }
        }

        return ok;
    }

    public float sum() {
        float s = 0.0f;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                s += value[i][j];
            }
        }
        return s;
    }

    public void loopMatMul(Leaf a, Leaf b) {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                for (int k = 0; k < size; k++) {
                    value[i][j] += a.value[i][k] * b.value[k][j];
                }
            }
        }
    }

    /** 
     * Version of matrix multiplication that steps 2 rows and columns
     * at a time. Adapted from Cilk demos.
     * Note that the results are added into C, not just set into C.
     * This works well here because Java array elements
     * are created with all zero values.
     **/
    void multiplyStride2(Leaf a, Leaf b) {
        for (int j = 0; j < size; j += 2) {
            for (int i = 0; i < size; i += 2) {

                float[] a0 = a.value[i];
                float[] a1 = a.value[i + 1];

                float s00 = 0.0F;
                float s01 = 0.0F;
                float s10 = 0.0F;
                float s11 = 0.0F;

                for (int k = 0; k < size; k += 2) {

                    float[] b0 = b.value[k];

                    s00 += a0[k] * b0[j];
                    s10 += a1[k] * b0[j];
                    s01 += a0[k] * b0[j + 1];
                    s11 += a1[k] * b0[j + 1];

                    float[] b1 = b.value[k + 1];

                    s00 += a0[k + 1] * b1[j];
                    s10 += a1[k + 1] * b1[j];
                    s01 += a0[k + 1] * b1[j + 1];
                    s11 += a1[k + 1] * b1[j + 1];
                }

                value[i][j] += s00;
                value[i][j + 1] += s01;
                value[i + 1][j] += s10;
                value[i + 1][j + 1] += s11;
            }
        }
    }
}