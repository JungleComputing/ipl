strictfp class BarnesHut {

	public static final boolean DEBUG = true;

	BarnesHut() {

	}

	void start() {
		Plummer p = new Plummer();
		Body[] bodies = new Body[8];

		bodies[0] = new Body(-2.0, -2.0, -2.0);
		bodies[1] = new Body(-2.0, -1.0, 2.0);
		bodies[2] = new Body(-2.0, 2.0, -2.0);
		bodies[3] = new Body(-2.0, 1.0, 2.0);
		bodies[4] = new Body(2.0, -2.0, -2.0);
		bodies[5] = new Body(2.0, -1.0, 2.0);
		bodies[6] = new Body(2.0, 2.0, -2.0);
		bodies[7] = new Body(2.0, 1.0, 2.0);

		bodies = p.generate(30);
		
		BodyTreeNode bt = new BodyTreeNode(bodies);
		bt.print(System.out);

		BodyCanvas bc = Body.visualizeArray(bodies);
		//wait4key();
		//bc.setBodies(p.generate(100));
	}

	void wait4key() {
		System.out.print("Press enter..");
		try {
			System.in.read();
		} catch (Exception e) {
			System.out.println("EEK: " + e);
		}
		System.out.println("Thnxz");
	}

	public static void main(String argv[]) {
		new BarnesHut().start();
	}
}
