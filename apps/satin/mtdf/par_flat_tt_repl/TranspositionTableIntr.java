public interface TranspositionTableIntr extends java.rmi.Remote {
    void remoteStore(int[] aindex, long[] atag, short[] avalue,
            short[] abestChild, byte[] adepth, boolean[] alowerBound)
            throws java.rmi.RemoteException;
}