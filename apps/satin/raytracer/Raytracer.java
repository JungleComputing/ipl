import java.io.*;



class Raytracer { // implements java.io.Serializable {
    //	World world;
    static int width  = 512;
    static int height = 512;
    static Surface current_surf = new Surface();
    static World dummy          = new World();
    String input;
    boolean dump;

    static Vector3D ReadVector(StreamTokenizer d) throws IOException {
	Vector3D v = new Vector3D(0, 0, 1);

	v.x = read_double(d);
	v.y = read_double(d);
	v.z = read_double(d);

	return v;
    }

    static GfxColor ReadColor(StreamTokenizer d) throws IOException {
	GfxColor c = new GfxColor(0, 0, 1);

	c.r = read_double(d);
	c.g = read_double(d);
	c.b = read_double(d);

	return c;
    }

    static float read_double(StreamTokenizer d) throws IOException {
	d.nextToken();

	double x = d.nval;

	d.nextToken();
	if (d.sval != null && d.sval.charAt(0) == 'e') {
	    String sub = d.sval.substring( 1 );
	    x = Math.pow(x, Double.parseDouble(sub));
	} else {
	    d.pushBack();
	}

	return (float) x;
    }

    static void ReadDescriptor(StreamTokenizer d) throws IOException {
	if (d.ttype == StreamTokenizer.TT_EOF || d.ttype == StreamTokenizer.TT_EOL) {
	    System.out.println("Hmmm.\n");
	    System.exit(1);
	}

	if (d.sval.equals("v")) {
	    d.nextToken();  
	    dummy.camera_pos = ReadVector(d);
	    d.nextToken();  
	    dummy.camera_target  = ReadVector(d);
	    d.nextToken();  
	    dummy.camera_up = ReadVector(d);
	    d.nextToken();  
	    dummy.view_angle = read_double(d);
	    d.nextToken();  
	    dummy.hither = read_double(d);
	    d.nextToken();  

	    width  = dummy.width  = (int) read_double(d);
	    height = dummy.height = (int) read_double(d);
	} else if (d.sval.equals("b")) {
	    dummy.background = ReadColor(d);
	} else if (d.sval.equals("l")) {
	    dummy.AddLight(new Light3D(dummy,
			ReadVector(d),
			0.0001f,
			new WhiteSurface(),
			World.DEFAULT_LIGHT_INTENSITY));
	} else if (d.sval.equals("f")) {
	    current_surf.color      = ReadColor(d);
	    current_surf.diffusity  = read_double(d);
	    current_surf.specular   = read_double(d);
	    current_surf.shine      = read_double(d);
	    current_surf.T          = read_double(d);  
	    current_surf.refraction = read_double(d);
	} else if (d.sval.equals("c")) {
	    Cone3D c = new Cone3D(dummy,
		    current_surf,
		    ReadVector(d), 
		    read_double(d),
		    ReadVector(d),
		    read_double(d));
	    dummy.AddObject(c);

	} else if (d.sval.equals("s")) {
	    Sphere3D s = new Sphere3D(dummy,
		    ReadVector(d),
		    read_double(d),
		    current_surf);
	    dummy.AddObject(s);
	} else if (d.sval.equals("p")) {
	    int p = (int) read_double(d);

	    Vector3D [] points = new Vector3D[p];

	    for (int i=0;i<p;i++) {
		points[i] = ReadVector(d);
	    }

	    Polygon3D bla = new Polygon3D(dummy, current_surf, p, points);
	    dummy.AddObject(bla);
	} else if (d.sval.equals("pp")) {
	    int n = (int) read_double(d);
	    if (n != 3) {
		System.out.println("unhandled\n");
		System.exit(1);
	    }

	    Triangle3D t = new TrianglePatch3D(dummy,
		    ReadVector(d),
		    ReadVector(d),
		    ReadVector(d),
		    ReadVector(d),
		    ReadVector(d),
		    ReadVector(d),
		    current_surf);

	    dummy.AddObject(t);
	} else {
	    System.out.println("Parse error:"+d.sval);
	    System.exit(1);
	}
    }

    static void ReadFile(String file) {
	try {

	    FileInputStream s = new FileInputStream(file);
	    StreamTokenizer d = new StreamTokenizer(new InputStreamReader(s));

	    d.commentChar('#');

	    while (d.nextToken() != StreamTokenizer.TT_EOF) {
		ReadDescriptor(d);
	    }

	} catch (Exception e) {
	    System.out.println("ReadFile Error: " + e);
	    e.printStackTrace();
	    System.exit(1);
	}
    }

    public static void main(String arg[]) {
	String input = null;
	boolean dump = false;
	boolean test = false;

	for (int i=0;i<arg.length;i++) {
	    if(arg[i].equals("-dump")) {
		dump = true;
	    } else if (input == null) {
		input = arg[i];
	    } else {
		System.out.println("Usage: java Raytracer [-dump] <input file>");
		System.exit(1);
	    }
	}
	if (input == null) {
	    input = "pics" + System.getProperty("file.separator") + "balls2_small.nff";
	    test = true;
	}

	ReadFile(input);

	Raytracer raytracer = new Raytracer(dump, input, test);

    }


    public Raytracer(boolean dump, String input, boolean test) {
	VirtualScreen s;

	this.dump = dump;
	this.input = input;

	//world = dummy; // new World(width, height, dummy);
	dummy.init();

	System.out.println("Raytracer started, input file = " + input);
	long start = System.currentTimeMillis();
	s = /*SPAWN*/  dummy.DivideAndConquer(0, 0, width, height);
	dummy.sync();
	long end = System.currentTimeMillis();
	double time = ((double) end - start) / 1000.0;

	System.out.println("application time raytrace (" + input + ") took " + time + 
		" s");

	System.out.println("application result raytrace (" + input + ") = OK");

	if(dump) {
	    System.out.println("Dumping output to out.ppm");
	    s.Dump("out.ppm");
	    System.out.println("Done");
	}

	if (test) {
	    if (s.check("test_goal.ppm")) {
		System.out.println("Test succeeded!");
	    }
	    else {
		System.out.println("Test failed!");
		System.exit(1);
	    }
	}
    }
}
