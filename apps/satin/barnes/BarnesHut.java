strictfp class BarnesHut {

	public static final boolean DEBUG = true;

	static final double START_TIME = 0.0;
	static final double DEFAULT_END_TIME = 10.0;
	static final double DEFAULT_DT = 0.025;
	static final double DEFAULT_THETA = 2.0;
	static final double SOFT_SQ = 0.00000000000625;

	final Body[] bodies;
	final double DT;
	final double END_TIME;
	final double THETA;
	final int ITERATIONS;

	BarnesHut(Body[] bodies) {
		this.bodies = bodies;

		//some magic copied from the RMI version...
		double scale = Math.pow( bodies.length / 16384.0 , -0.25 );
		DT = DEFAULT_DT * scale;
		END_TIME = DEFAULT_END_TIME * scale;
		THETA = DEFAULT_THETA / scale;

		ITERATIONS = (int)(((END_TIME - START_TIME) / DT) + 1.1);
	}

	void run() {
		BodyTreeNode root = new BodyTreeNode(bodies, THETA);
		root.print(System.out);
		BodyCanvas bc = Body.visualizeArray(bodies);

		for (int j = 0; j < 100; j++) {
			root.computeCentersOfMass();
			for (int i = 0; i < bodies.length; i++) {
				root.barnes(bodies[i]);
				bodies[i].computeNewPosition(false, DT);
			}
			wait4key();
			bc.setBodies(bodies);
		}
		root.print(System.out);
	}

	void wait4key() {
		System.out.print("Press enter..");
		try {
			System.in.read();
		} catch (Exception e) {
			System.out.println("EEK: " + e);
		}
	}

	public static void main(String argv[]) {
		Body[] bodies;

		bodies = new Body[2];
		bodies[0] = new Body(-2.0, -2.0, -2.0, 0.0, 0.0, 0.0);
		
		bodies[1] = new Body(2.0, 2.0, 2.0, 0.0, 0.0, 0.0);


		/*bodies[2] = new Body(-2.0, 2.0, -2.0);
		bodies[3] = new Body(-2.0, 1.0, 2.0);
		bodies[4] = new Body(2.0, -2.0, -2.0);
		bodies[5] = new Body(2.0, -1.0, 2.0);
		bodies[6] = new Body(2.0, 2.0, -2.0);
		bodies[7] = new Body(2.0, 1.0, 2.0); */

		bodies = new Plummer().generate(20);

		new BarnesHut(bodies).run();
	}
}
