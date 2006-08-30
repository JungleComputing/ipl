/* $Id$ */

interface MmultInterface extends ibis.satin.Spawnable {
    public Matrix mult(int task, int rec, SharedMatrix a, byte[] aPos, SharedMatrix b, byte[] bPos, Matrix c);
}