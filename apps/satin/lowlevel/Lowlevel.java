// do the inlet test in a seperate method, as multiple inlets in a method are more expensive.
final class Lowlevel extends ibis.satin.SatinObject implements LowlevelInterface, java.io.Serializable  {

	static DoneVoid doneVoid = new DoneVoid();
	static DoneInt doneInt = new DoneInt(0);
	static DoneObject doneObject = new DoneObject(new Object());

	public void spawn_void() {
		return;
	}

	public int spawn_int(int i) {
		return i;
	}

	public Object spawn_Object(Object o) {
		return o;
	}

	public void call_void() {
		return;
	}

	public int call_int(int i) {
		return i;
	}

	public Object call_Object(Object o) {
		return o;
	}

	public void inlet_spawn_void() throws DoneVoid {
		throw new DoneVoid();
	}

	public void inlet_spawn_int(int i) throws DoneInt {
		throw new DoneInt(i);
	}

	public void inlet_spawn_Object(Object o) throws DoneObject {
		throw new DoneObject(o);
	}

	public void exception_call_void() throws DoneVoid {
		throw new DoneVoid();
	}

	public void exception_call_int(int i) throws DoneInt {
		throw new DoneInt(i);
	}

	public void exception_call_Object(Object o) throws DoneObject {
		throw new DoneObject(o);
	}

	public void inlet_spawn_void_no_new() throws DoneVoid {
		throw Lowlevel.doneVoid;
	}

	public void inlet_spawn_int_no_new(int i) throws DoneInt {
		throw Lowlevel.doneInt;
	}

	public void inlet_spawn_Object_no_new(Object o) throws DoneObject {
		throw Lowlevel.doneObject;
	}

	public void inlet_spawn_void_overridden() throws DoneVoidOverridden {
		throw new DoneVoidOverridden();
	}

	public void inlet_spawn_int_overridden(int i) throws DoneIntOverridden {
		throw new DoneIntOverridden(i);
	}

	public void inlet_spawn_Object_overridden(Object o) throws DoneObjectOverridden {
		throw new DoneObjectOverridden(o);
	}

	public void exception_call_void_no_new() throws DoneVoid {
		throw Lowlevel.doneVoid;
	}

	public void exception_call_int_no_new(int i) throws DoneInt {
		throw Lowlevel.doneInt;
	}

	public void exception_call_Object_no_new(Object o) throws DoneObject {
		throw Lowlevel.doneObject;
	}

	public static void doVoidExceptionCallTest(Lowlevel l, int n) {
		long start, end;
		double time;

		start = System.currentTimeMillis();
		for(int i=0; i<n; i++) {
			try {
				l.exception_call_void();
			} catch (Throwable e) {}
		}
		end = System.currentTimeMillis();
		time = (double)(end - start);
		
		System.out.println("exception call void took " + (time / 1000.0) + " seconds, " +
				   ((time * 1000.0) / n) + " micro's per call");
	}

	public static void doIntExceptionCallTest(Lowlevel l, int n) {
		long start, end;
		double time;

		start = System.currentTimeMillis();
		for(int i=0; i<n; i++) {
			try {
				l.exception_call_int(0);
			} catch (Throwable e) {}
		}
		end = System.currentTimeMillis();
		time = (double)(end - start);
		
		System.out.println("exception call int took " + (time / 1000.0) + " seconds, " +
				   ((time * 1000.0) / n) + " micro's per call");
	}

	public static void doObjectExceptionCallTest(Lowlevel l, int n) {
		long start, end;
		double time;
		Object o = new Object();

		start = System.currentTimeMillis();
		for(int i=0; i<n; i++) {
			try {
				l.exception_call_Object(o);
			} catch (Throwable e) {}
		}
		end = System.currentTimeMillis();
		time = (double)(end - start);
		
		System.out.println("exception call object took " + (time / 1000.0) + " seconds, " +
				   ((time * 1000.0) / n) + " micro's per call");
	}

	public static void doVoidInletSpawnTest(Lowlevel l, int n) {
		long start, end;
		double time;

		start = System.currentTimeMillis();
		for(int i=0; i<n; i++) {
			try {
				l.inlet_spawn_void();
			} catch (Throwable e) {return;}
			l.sync();
		}
		end = System.currentTimeMillis();
		time = (double)(end - start);
		
		System.out.println("inlet spawn void took " + (time / 1000.0) + " seconds, " +
				   ((time * 1000.0) / n) + " micro's per call");
	}

	public static void doIntInletSpawnTest(Lowlevel l, int n) {
		long start, end;
		double time;

		start = System.currentTimeMillis();
		for(int i=0; i<n; i++) {
			try {
				l.inlet_spawn_int(0);
			} catch (Throwable e) {return;}
			l.sync();
		}
		end = System.currentTimeMillis();
		time = (double)(end - start);
		
		System.out.println("inlet spawn int took " + (time / 1000.0) + " seconds, " +
				   ((time * 1000.0) / n) + " micro's per call");
	}

	public static void doObjectInletSpawnTest(Lowlevel l, int n) {
		long start, end;
		double time;
		Object o = new Object();

		start = System.currentTimeMillis();
		for(int i=0; i<n; i++) {
			try {
				l.inlet_spawn_Object(o);
			} catch (Throwable e) {return;}
			l.sync();
		}
		end = System.currentTimeMillis();
		time = (double)(end - start);
		
		System.out.println("inlet spawn object took " + (time / 1000.0) + " seconds, " +
				   ((time * 1000.0) / n) + " micro's per call");
	}

	public static void doVoidInletSpawnOverriddenTest(Lowlevel l, int n) {
		long start, end;
		double time;

		start = System.currentTimeMillis();
		for(int i=0; i<n; i++) {
			try {
				l.inlet_spawn_void_overridden();
			} catch (Throwable e) {return;}
			l.sync();
		}
		end = System.currentTimeMillis();
		time = (double)(end - start);
		
		System.out.println("inlet spawn void overridden took " + (time / 1000.0) + " seconds, " +
				   ((time * 1000.0) / n) + " micro's per call");
	}

	public static void doIntInletSpawnOverriddenTest(Lowlevel l, int n) {
		long start, end;
		double time;

		start = System.currentTimeMillis();
		for(int i=0; i<n; i++) {
			try {
				l.inlet_spawn_int_overridden(0);
			} catch (Throwable e) {return;}
			l.sync();
		}
		end = System.currentTimeMillis();
		time = (double)(end - start);
		
		System.out.println("inlet spawn int overridden took " + (time / 1000.0) + " seconds, " +
				   ((time * 1000.0) / n) + " micro's per call");
	}

	public static void doObjectInletSpawnOverriddenTest(Lowlevel l, int n) {
		long start, end;
		double time;
		Object o = new Object();

		start = System.currentTimeMillis();
		for(int i=0; i<n; i++) {
			try {
				l.inlet_spawn_Object_overridden(o);
			} catch (Throwable e) {return;}
			l.sync();
		}
		end = System.currentTimeMillis();
		time = (double)(end - start);
		
		System.out.println("inlet spawn object overridden took " + (time / 1000.0) + " seconds, " +
				   ((time * 1000.0) / n) + " micro's per call");
	}

	public static void doVoidExceptionCallTestNoNew(Lowlevel l, int n) {
		long start, end;
		double time;

		start = System.currentTimeMillis();
		for(int i=0; i<n; i++) {
			try {
				l.exception_call_void_no_new();
			} catch (Throwable e) {}
		}
		end = System.currentTimeMillis();
		time = (double)(end - start);
		
		System.out.println("exception call void no new took " + (time / 1000.0) + " seconds, " +
				   ((time * 1000.0) / n) + " micro's per call");
	}

	public static void doIntExceptionCallTestNoNew(Lowlevel l, int n) {
		long start, end;
		double time;

		start = System.currentTimeMillis();
		for(int i=0; i<n; i++) {
			try {
				l.exception_call_int_no_new(0);
			} catch (Throwable e) {}
		}
		end = System.currentTimeMillis();
		time = (double)(end - start);
		
		System.out.println("exception call int no new took " + (time / 1000.0) + " seconds, " +
				   ((time * 1000.0) / n) + " micro's per call");
	}

	public static void doObjectExceptionCallTestNoNew(Lowlevel l, int n) {
		long start, end;
		double time;
		Object o = new Object();

		start = System.currentTimeMillis();
		for(int i=0; i<n; i++) {
			try {
				l.exception_call_Object_no_new(o);
			} catch (Throwable e) {}
		}
		end = System.currentTimeMillis();
		time = (double)(end - start);
		
		System.out.println("exception call object no new took " + (time / 1000.0) + " seconds, " +
				   ((time * 1000.0) / n) + " micro's per call");
	}

	public static void doVoidInletSpawnTestNoNew(Lowlevel l, int n) {
		long start, end;
		double time;

		start = System.currentTimeMillis();
		for(int i=0; i<n; i++) {
			try {
				l.inlet_spawn_void_no_new();
			} catch (Throwable e) {return;}
			l.sync();
		}
		end = System.currentTimeMillis();
		time = (double)(end - start);
		
		System.out.println("inlet spawn void no new took " + (time / 1000.0) + " seconds, " +
				   ((time * 1000.0) / n) + " micro's per call");
	}

	public static void doIntInletSpawnTestNoNew(Lowlevel l, int n) {
		long start, end;
		double time;

		start = System.currentTimeMillis();
		for(int i=0; i<n; i++) {
			try {
				l.inlet_spawn_int_no_new(0);
			} catch (Throwable e) {return;}
			l.sync();
		}
		end = System.currentTimeMillis();
		time = (double)(end - start);
		
		System.out.println("inlet spawn int no new took " + (time / 1000.0) + " seconds, " +
				   ((time * 1000.0) / n) + " micro's per call");
	}

	public static void doObjectInletSpawnTestNoNew(Lowlevel l, int n) {
		long start, end;
		double time;
		Object o = new Object();

		start = System.currentTimeMillis();
		for(int i=0; i<n; i++) {
			try {
				l.inlet_spawn_Object_no_new(o);
			} catch (Throwable e) {return;}
			l.sync();
		}
		end = System.currentTimeMillis();
		time = (double)(end - start);
		
		System.out.println("inlet spawn object no new took " + (time / 1000.0) + " seconds, " +
				   ((time * 1000.0) / n) + " micro's per call");
	}

	public static void doNewTest(int n) {
		long start, end;
		double time;
		Object x = new Object();

		start = System.currentTimeMillis();
		for (int i=0; i<n; i++) {
			Object o = new Object();
		}
		end = System.currentTimeMillis();
		time = (double)(end - start);
		
		System.out.println("object new took " + (time / 1000.0) + " seconds, " +
				   ((time * 1000.0) / n) + " micro's per call");

		start = System.currentTimeMillis();
		for (int i=0; i<n; i++) {
			Throwable t = new Throwable();
		}
		end = System.currentTimeMillis();
		time = (double)(end - start);
		
		System.out.println("throwable new took " + (time / 1000.0) + " seconds, " +
				   ((time * 1000.0) / n) + " micro's per call");

		start = System.currentTimeMillis();
		for (int i=0; i<n; i++) {
			DoneVoidOverridden o = new DoneVoidOverridden();
		}
		end = System.currentTimeMillis();
		time = (double)(end - start);
		
		System.out.println("overridden throwable new void took " + (time / 1000.0) + " seconds, " +
				   ((time * 1000.0) / n) + " micro's per call");

		start = System.currentTimeMillis();
		for (int i=0; i<n; i++) {
			DoneIntOverridden o = new DoneIntOverridden(0);
		}
		end = System.currentTimeMillis();
		time = (double)(end - start);
		
		System.out.println("overridden throwable new int took " + (time / 1000.0) + " seconds, " +
				   ((time * 1000.0) / n) + " micro's per call");

		start = System.currentTimeMillis();
		for (int i=0; i<n; i++) {
			DoneObjectOverridden o = new DoneObjectOverridden(x);
		}
		end = System.currentTimeMillis();
		time = (double)(end - start);
		
		System.out.println("overridden throwable new Object took " + (time / 1000.0) + " seconds, " +
				   ((time * 1000.0) / n) + " micro's per call");

	}

	public static void doCallTest(Lowlevel l, int n) {
		long start, end;
		double time;
		Object o = new Object();

		start = System.currentTimeMillis();
		for(int i=0; i<n; i++) {
			l.call_void();
		}
		end = System.currentTimeMillis();
		time = (double)(end - start);
			
		System.out.println("call void took " + (time / 1000.0) + " seconds, " +
				   ((time * 1000.0) / n) + " micro's per call");
			
			
		start = System.currentTimeMillis();
		for(int i=0; i<n; i++) {
			int res = l.call_int(0);
		}
		end = System.currentTimeMillis();
		time = (double)(end - start);
			
		System.out.println("call int took " + (time / 1000.0) + " seconds, " +
				   ((time * 1000.0) / n) + " micro's per call");


		start = System.currentTimeMillis();
		for(int i=0; i<n; i++) {
			Object res = l.call_Object(o);
		}
		end = System.currentTimeMillis();
		time = (double)(end - start);
			
		System.out.println("call Object took " + (time / 1000.0) + " seconds, " +
				   ((time * 1000.0) / n) + " micro's per call");

	}

	public static void doSpawnTest(Lowlevel l, int n) {
		long start, end;
		double time;
		Object o = new Object();

		start = System.currentTimeMillis();
		for(int i=0; i<n; i++) {
			l.spawn_void();
			l.sync();
		}
		end = System.currentTimeMillis();
		time = (double)(end - start);
			
		System.out.println("spawn sync void took " + (time / 1000.0) + " seconds, " +
				   ((time * 1000.0) / n) + " micro's per call");


		start = System.currentTimeMillis();
		for(int i=0; i<n; i++) {
			int res = l.spawn_int(0);
			l.sync();
		}
		end = System.currentTimeMillis();
		time = (double)(end - start);
			
		System.out.println("spawn sync int took " + (time / 1000.0) + " seconds, " +
				   ((time * 1000.0) / n) + " micro's per call");


		start = System.currentTimeMillis();
		for(int i=0; i<n; i++) {
			Object res = l.spawn_Object(o);
			l.sync();
		}
		end = System.currentTimeMillis();
		time = (double)(end - start);
			
		System.out.println("spawn sync Object took " + (time / 1000.0) + " seconds, " +
				   ((time * 1000.0) / n) + " micro's per call");
	}

	public static void main(String[] args) {
		long start, end;
		double time;
		Object o = new Object();

		if(args.length != 1) {
			System.err.println("application lowlevel got wrong number of parameters, need one");
			System.exit(1);
		}
		int n = Integer.parseInt(args[0]);

		Lowlevel l = new Lowlevel();

		System.out.println("Running Lowlevel " + n);

		for(int iter=0; iter<3; iter++) {
                        System.out.println("\n---iter " + iter + "---"); 

                        System.out.println("---news---"); 
			doNewTest(n);

                        System.out.println("---calls---"); 
			doCallTest(l, n);

                        System.out.println("---exception calls---"); 
			doVoidExceptionCallTest(l, n);
			doIntExceptionCallTest(l, n);
			doObjectExceptionCallTest(l, n);

			doVoidExceptionCallTestNoNew(l, n);
			doIntExceptionCallTestNoNew(l, n);
			doObjectExceptionCallTestNoNew(l, n);

                        System.out.println("---spawns---"); 
			doSpawnTest(l, n);

                        System.out.println("---inlet spawns---"); 
			doVoidInletSpawnTest(l, n);
			doIntInletSpawnTest(l, n);
			doObjectInletSpawnTest(l, n);

			doVoidInletSpawnTestNoNew(l, n);
			doIntInletSpawnTestNoNew(l, n);
			doObjectInletSpawnTestNoNew(l, n);

			doVoidInletSpawnOverriddenTest(l, n);
			doIntInletSpawnOverriddenTest(l, n);
			doObjectInletSpawnOverriddenTest(l, n);
		}
	}
}
