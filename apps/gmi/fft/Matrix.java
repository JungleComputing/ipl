class Matrix {

    private double[] matrix;
    private int size;

    int cpu; 

    Matrix(int cpu, int size) {
	this.size = size;
	//	matrix = new double[size * size * 2];
	this.cpu = cpu;
	//	for (int i = size*size*2-1; i >= 0; i--)
	//	    matrix[i] = 0.0;
    }

    double[] getValues() {
	return matrix;
    }

    void setValues(double[] values) {
	matrix = values;
    }

    void copyValues(double[] values) {
	if (matrix == null) { 
	    matrix = new double[size * size * 2];
	}

	for (int i = size*size*2-1; i >= 0; i--)
	    matrix[i] = values[i];

    }

    double[] Copy() {
	double[] m = new double[size*size*2];
	for (int i = size*size*2-1; i >= 0; i--)
	    m[i] = matrix[i];
	return m;
    }

    double checksum() {
	double checksum = 0.0;
	for (int i = size*size*2-1; i >= 0; i--)
	    checksum += matrix[i];
	return checksum;
    }

    void transpose() {
	int i_size = 0;
	for (int i = 0; i < size; i++) {
	    int j_size = 0;
	    for (int j = 0; j < i; j++) {
		int index1 = (j_size + i) * 2;
		int index2 = (i_size + j) * 2;
		double tmp = matrix[index1];
		matrix[index1] = matrix[index2];
		matrix[index2] = tmp;
		index1++;
		index2++;
		tmp = matrix[index1];
		matrix[index1] = matrix[index2];
		matrix[index2] = tmp;
		j_size += size;
	    }
	    i_size += size;
	}
    }

    void print() {
	int tot = size * size * 2;
	for (int i = size*size*2-1; i >= 0; i--)
	    System.out.println(matrix[i]);
    }
}
