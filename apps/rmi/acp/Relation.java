class Relation implements java.io.Serializable {

	boolean rel[][];
	int dimension;
	int number;

	Relation(int dim, int num, OrcaRandom random, int pairs, int values) {

		int m,n;

		dimension = dim;
		number    = num;

		rel = new boolean[dim][dim];

		for (int i=0;i<dim;i++) {
			for (int j=0;j<dim;j++) {
				rel[i][j] = false;
			}
		}

		rel[dim-1][dim-1] = true;

		for (int i=0;i<pairs-1;i++) {
			do {
				// pick a location m, n at random, but check 
				// that m != n and !rel[m][n];
				m = random.val() % (values-1);
				n = random.val() % (values-1);

			} while (rel[m][n] || m == n);

			rel[m][n] = true;
		}			       	       
	}

	boolean test(int val1, int val2) {		
		return rel[val1][val2];
	}

	public String toString() {

		String temp = "Relation " + number + "\n";
		
		for (int i=0;i<dimension;i++) {
			for (int j=0;j<dimension;j++) {
				if (rel[i][j]) {
					temp += "1";
				} else {
					temp += "0";
				}
			}
			temp += "\n";
		}	

		return temp;
	}
}
	
