interface MmultInterface extends ibis.satin.Spawnable {
	public Matrix mult(int task, int rec, Matrix a, Matrix b, Matrix c);
}
