interface NQueensInterface extends ibis.satin.Spawnable {
    public void nqueens(byte[] sofar, int row, int size) throws Abort;
}