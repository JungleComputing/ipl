import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import ibis.util.PoolInfo;

class Slave extends UnicastRemoteObject implements SlaveInterface {

    static final double FORWARD = 1.0;
    static final double BACKWARD = -1.0;

    private int myCpu, cpus, N, M, rootN, rowsperproc;
    private double[] u, u2;
    private int[][] ind1, ind2;
    private MasterInterface master;
    private long transposeAndFFT, fftDuration, totalComDuration, barDuration;
    private Matrix[] matrixArray;
    private SlaveInterface[] slaveArray;
    private int[][] distribution;

//    public static native void fs_stats_reset();

    Slave(String masterName, int cpu, int cpus, int N, int M,
	  int rootN, int rowsperproc, double[] u, double[] u2,
	  int[][] dis, PoolInfo d, int rounds) throws Exception {

	while (true) {
	    Thread.sleep(500);
	    try {
		master = (MasterInterface)Naming.lookup("//" + masterName + "/FFT_Master");
		System.out.println(cpu + ": Slave has found Master " + master);
		break;
	    } catch (Exception e) {
		System.out.println(cpu + ": lookup master -- sleep a while...");
	    }
	}
	myCpu = cpu;
	this.cpus = cpus;
	this.N = N;
	this.M = M;
	this.rootN = rootN;
	this.rowsperproc = rowsperproc;
	this.u = u;
	this.u2 = u2;
	this.distribution = dis;
	transposeAndFFT = 0;
	fftDuration = 0;
	totalComDuration = 0;
	barDuration = 0;

	matrixArray = new Matrix[cpus];
	for (int i = 0; i < cpus; i++) {
	    matrixArray[i] = new Matrix(rowsperproc);
	}
	slaveArray = master.table((SlaveInterface) this, myCpu);

	// System.err.println(myCpu + ": gonna hit barrier I");

	long total = 0;

	for (int warmup = 0; warmup < rounds; warmup++) {

	    if (cpus > 1) master.sync();

	    // System.err.println(myCpu + ": past barrier I");

	    int myFirst = rootN * myCpu / cpus;
	    int myLast = rootN * (myCpu + 1) / cpus;
	    ind1 = new int[cpus][cpus];
	    ind2 = new int[cpus][cpus];
	    initIndexBlocks(ind1, ind2);
	    // System.err.println(myCpu + ": Past initIndexBlocks");
	    initX(ind1, myFirst);

	    // System.err.println(myCpu + ": gonna hit barrier II");

	    if (cpus > 1) master.sync();

	    // System.err.println(myCpu + ": past barrier II, start clock");

	    double checksum1 = checksum();

	    long start = System.currentTimeMillis();

	    // System.gc();

	    doFFT(FORWARD,  u, u2, myFirst, myLast);
	    double checksum3 = checksum();
	    doFFT(BACKWARD, u, u2, myFirst, myLast);
	    double checksum2 = checksum();

	    if (cpus > 1) master.sync();

	    long end = System.currentTimeMillis();

	    total += end - start;

	    if (Math.abs(checksum1 - checksum2) > 0.1) {
		    System.out.println("CPU" + myCpu + " checksum ERROR !!! (" +
				       checksum1 + "," + checksum2 + ")   " + checksum3);
	    } else {
//		System.out.println("CPU" + myCpu + " checksum OK (" + 
//				   checksum1+","+checksum2+")   " + checksum3);

//		System.out.println(myCpu + "-> FFT: " + fftDuration + "\t Trans: " +
//				   transposeAndFFT + "\t Communication: " +
//				   totalComDuration + "\t Barriers: " + barDuration +
//				   " ms.\t     ");
	    }

	    if (myCpu == 0) { 
		    System.out.println("FFT round " + warmup + " time " + (end - start) / 1000.0);
	    }
	}
	//d.printTime("FFT, M = " + M, transposeAndFFT);

	if (myCpu == 0) { 
		long per_round = total / rounds;
		System.out.println("FFT, iterate " + rounds + "; M = " + M + " time " + total / 1000.0 + "; per round " + per_round / 1000.0);
	}
    }


    void initIndexBlocks(int[][] ind1, int[][] ind2) {
	for (int i = 0; i < cpus; i++)
	    for (int j = 0; j < cpus; j++) {
		ind1[i][j] = i * cpus + j;
		ind2[j][i] = i * cpus + j;
	    }
    }


    void initX(int[][] ind, int myFirst) {
	double[] matrix = new double[rowsperproc * rootN * 2];

	for (int i = 0; i < rootN; i++)
	    for (int j = 0; j < rowsperproc; j++) {
		int index = 2 * (i + j * rootN);
		double value = myFirst + j + i * rootN + 0.1;
		matrix[index] = value;
		matrix[index + 1] = value + 1000.0;
	    }

	pageOut(matrix, ind);
    }


    void pageOut(double[] x, int[][] ind) {
	try {
//	    long start = System.currentTimeMillis();
//	    double[] matrix = master.getMatrixValues(ind[myCpu][0]);
	    int slaveNr = distribution[0][ ind[myCpu][0] ];
	    int matrixNr =distribution[0][ ind[myCpu][0] ];

//          PERFORMANCE BUG ??? 
//
//	    double[] matrix;

//	    if (myCpu == slaveNr) {
//		matrix = getCopyValues(matrixNr);
//	    } else { 
//		    matrix = slaveArray[slaveNr].getValues(matrixNr);
//	    } 
//
//    
	    double [] matrix = new double[rowsperproc*rowsperproc*2];

	    for (int i = 0; i < rootN; i++) {
		for (int j = 0; j < rowsperproc; j++) {
		    int index = ((i % rowsperproc) + j * rowsperproc) * 2;
		    matrix[index] = x[(i + rootN * j) * 2];
		    matrix[index + 1] =  x[(i + rootN * j) * 2 + 1];
		}
		if (i % rowsperproc == (rowsperproc - 1)) {
//		    master.setMatrixValues(ind[myCpu][i / rowsperproc], matrix);
		    slaveNr = distribution[0][ ind[myCpu][i / rowsperproc] ];
		    matrixNr =distribution[1][ ind[myCpu][i / rowsperproc] ];
		    if (slaveNr == myCpu) {
			// System.err.print("=" + slaveNr);
			setCopyValues(matrixNr, matrix);
			// System.err.print(">");
//			System.out.println(slaveNr + "Exchange got matrix " + matrixNr + " " + matrix[0]); 
		    } else { 
			// System.err.print("<" + slaveNr);
			  slaveArray[slaveNr].setValues(matrixNr, matrix);
			// System.err.print(">");
		    }
		    
		}
	    }
//	    totalComDuration += System.currentTimeMillis() - start;
	} catch (Exception e) {
	    System.out.println("Couldn't get/set matrix values from master " + e);
	    System.exit(1);
	}
    }


    void pageIn(double[] x, int[][] ind) {
	try {
//	    long start = System.currentTimeMillis();
	    if (cpus > 1) master.sync();
//	    barDuration += System.currentTimeMillis() - start;
//	    start = System.currentTimeMillis();
	    for (int i = 0; i < cpus; i++) {
//		double[] matrix = master.getMatrixValues(ind[myCpu][i]);
		int slaveNr = distribution[0][ ind[myCpu][i] ];
		int matrixNr =distribution[1][ ind[myCpu][i] ];
		double[] matrix;
	        if (myCpu == slaveNr) {
		    matrix = getCopyValues(matrixNr);
	        } else {
			matrix = slaveArray[slaveNr].getValues(matrixNr);
		}

//		System.out.println(myCpu + "Exchange got matrix " + matrixNr + " " + matrix[0]); 

		for (int j = 0; j < rowsperproc; j++) {
		    for (int k = 0; k < rowsperproc; k++) {
			int index1 = (i * rowsperproc + j + rootN * k) * 2;
			int index2 = (j + k * rowsperproc) * 2;
			x[index1] = matrix[index2];
			x[index1 + 1] = matrix[index2 + 1];
		    }
		}
	    }
//	    totalComDuration += System.currentTimeMillis() - start;
	} catch (Exception e) {
	    System.out.println("Couldn't get/set matrix values from master " + e);
	    System.exit(1);
	}
    }


    double checksum() {
	double checksum = 0.0;
	/*
	  try {
	    for (int i = 0; i < cpus; i++)
		checksum += master.getMatrixChecksum(myCpu * cpus + i);
	} catch (Exception e) {
	    System.out.println("Couldn't get checksum values from master " + e);
	    System.exit(1);
	}
	*/

	for (int i = 0; i < cpus; i++)
	    checksum += matrixArray[i].checksum();
	return checksum;
    }


    void doFFT(double direction, double[] u, double[] u2,
	       int myFirst, int myLast) throws Exception {

	if (direction == FORWARD) {
	    int[][] tmp = ind1;
	    ind1 = ind2;
	    ind2 = tmp;
	}

	double[] x = new double[cpus * rowsperproc * rowsperproc * 2];
	int m1 = M / 2;

//	long start = System.currentTimeMillis();
	transpose(ind1, ind2, x);

//	long start_FFT = System.currentTimeMillis();
	for (int i = myFirst; i < myLast; i++) {
	    fft1DOnce(direction, m1, rootN, i - myFirst, u, x);
	    twiddleOneCol(direction, i, i - myFirst, u2, x);
	}
//	fftDuration += System.currentTimeMillis() - start_FFT;
	pageOut(x, ind2);
	transpose(ind2, ind1, x);

//	start_FFT = System.currentTimeMillis();
	for (int i = myFirst; i < myLast; i++) {
	    fft1DOnce(direction, m1, rootN, i - myFirst, u, x);
	    if (direction == BACKWARD)
		scale(i - myFirst, x);
	}
//	fftDuration += System.currentTimeMillis() - start_FFT;

	pageOut(x, ind1);
	transpose(ind1, ind2, x);
//	transposeAndFFT += System.currentTimeMillis() - start;

	if (direction == FORWARD) {
	    int[][] tmp = ind1;
	    ind1 = ind2;
	    ind2 = tmp;
	}
    }


    public void doTranspose(int i) throws java.rmi.RemoteException {
	matrixArray[i].transpose();
    }


    void transpose(int[][] ind1, int[][] ind2, double[] matrix) {
	try {
//	    long start = System.currentTimeMillis();
	    if (cpus > 1) master.sync();
//	    barDuration += System.currentTimeMillis() - start;

	    for (int i = 0; i < cpus; i++) {
//		master.matrixTranspose(ind1[myCpu][i]);
//		int slaveNr = distribution[0][ ind1[myCpu][i] ];
//		int matrixNr = distribution[1][ ind1[myCpu][i] ];
//		slaveArray[slaveNr].doTranspose(matrixNr);
		    matrixArray[i].transpose();
	    }
	} catch (Exception e) {
	    System.out.println("Couldn't transpose matrix from master " + e);
	    System.exit(1);
	}

	pageIn(matrix, ind2);
    }


    void fft1DOnce(double direction, int M, int N, int column, double[] u,
		   double[] matrix) {
	reverse(M, N, column, matrix);

	for (int q = 1; q <= M; q++) {
	    int L = 1 << q;
	    int r = N / L;
	    int Lstar = L / 2;
	    int u_index = 2*(Lstar - 1);
	    for (int k = 0; k < r; k++) {
		int kmL = k*L;
		for (int j = 0; j < Lstar; j++) {
		    int row1 = kmL + j;
		    int row2 = row1 + Lstar;
		    int xxx1 = column * N * 2;
		    int xxx2 = u_index + (j << 1);
		    int cr2 = (row2 << 1) + xxx1;
		    int cr1 = (row1 << 1) + xxx1;
		    int cr2p = cr2 + 1;
		    int cr1p = cr1 + 1;
		    double omega_r = u[xxx2];
		    double omega_c = direction * u[xxx2 + 1];
		    double x_r = matrix[cr2];
		    double x_c = matrix[cr2p];
		    double tau_r = omega_r * x_r - omega_c * x_c;
		    double tau_c = omega_r * x_c + omega_c * x_r;
		    x_r = matrix[cr1];
		    x_c = matrix[cr1p];
		    matrix[cr2] = x_r - tau_r;
		    matrix[cr2p] = x_c - tau_c;
		    matrix[cr1] = x_r + tau_r;
		    matrix[cr1p] = x_c + tau_c;
		}
	    }
	}
    }


    void twiddleOneCol(double direction, int j, int column, double[] u2,
		       double[] matrix) {
	for (int i = 0; i < rootN; i++) {
	    double omega_r = u2[2 * (j * rootN + i)];
	    double omega_c = u2[2 * (j * rootN + i) + 1] * direction;
	    int index = 2 * (i + column * rootN);
	    double x_r = matrix[index];
	    double x_c = matrix[index + 1];
	    matrix[index] =     omega_r * x_r - omega_c * x_c;
	    matrix[index + 1] = omega_r * x_c + omega_c * x_r;
	}
    }


    void reverse(int M, int N, int column, double[] matrix) {
	int index1, index2;
	double temp;
	for (int k = 0; k < N; k++) {
	    int j = bitReverse(M, k);
	    if (j > k) {
		index1 = 2*(j + column*N);
		index2 = 2*(k + column*N);
		temp = matrix[index1];
		matrix[index1] = matrix[index2];
		matrix[index2] = temp;

		temp = matrix[index1 + 1];
		matrix[index1 + 1] = matrix[index2 + 1];
		matrix[index2 + 1] = temp;
	    }
	}
    }


    int bitReverse(int M, int k) {
	int j = 0;
	for (int i = 0; i < M; i++) {
	    j = (2 * j) + (k & 1);
	    k >>= 1;
	}
	return j;
    }


    void scale(int column, double[] matrix) {
	for (int i = 0; i < rootN; i++) {
	    int index = 2 * (i + column * rootN);
	    matrix[index]     /= N;
	    matrix[index + 1] /= N;
	}
    }

    public double[] getValues(int i)  throws RemoteException {
	// System.out.println("getValues " + i);
	// matrixArray[i].print();
	return matrixArray[i].getValues();
    }

    public double[] getCopyValues(int i)  throws RemoteException {
	return matrixArray[i].Copy();
    }

    public void setValues(int i, 
				double[] values) throws java.rmi.RemoteException {
//	System.out.println(myCpu + "slave got matrix " + i + " " + values[0]);
//	    System.out.println(myCpu + "Exchange got matrix " + i + " " + values[0]); 
	matrixArray[i].setValues(values);
	// matrixArray[i].print();
    }

    public void setCopyValues(int i, 
				double[] values) throws java.rmi.RemoteException {
	// System.out.println("setValues " + i);
	matrixArray[i].copyValues(values);
	// matrixArray[i].print();
    }

    public double getMatrixChecksum(int i)  throws java.rmi.RemoteException {
	return matrixArray[i].checksum();
    }

    public void matrixTranspose(int i)  throws java.rmi.RemoteException {
	matrixArray[i].transpose();
    }
}
