final class World extends ibis.satin.SatinObject implements WorldInterface, java.io.Serializable  {
    final static int MAX_RAY_DEPTH    = 5;
    final static double MAX_DIST      = 1000000;
    final static double MIN_DIST      = 0.000000001;
    final static double MIN_DIST_SQR  = MIN_DIST * MIN_DIST;

    final static float DEFAULT_LIGHT_INTENSITY = 0.5f;

    final static int fallout = 2;


    final static GfxColor RED   = new GfxColor(1f, 0.2f, 0.1f);
    final static GfxColor GREEN = new GfxColor(0f, 1f, 0f);
    final static GfxColor LIGHT_GREEN = new GfxColor(0.2f, 1f, 0.2f);
    final static GfxColor BLUE  = new GfxColor(0f, 0f, 1f);
    final static GfxColor WHITE = new GfxColor(0.9999f,0.9999f,0.9999f);
    final static GfxColor BLACK = new GfxColor(0f, 0f, 0f); 


    Vector3D camera_pos = new Vector3D(1, 1, 2);
    Vector3D camera_target = new Vector3D(0, 0, 0);
    Vector3D camera_up = new Vector3D(0, 1, 0);
    Vector3D look_vec;

    double view_angle, hither;

    GfxColor background = new GfxColor(0.9f, 0.9f, 0.2f);

    int objects, lights;
    Object3D HeadObjectList;
    Light3D  HeadLight;

    int mx, my, width, height;

    double cz;


    void init() {
	mx = width / 2;
	my = height / 2;
	cz = - mx / 2;

	look_vec = new Vector3D(camera_target.x - camera_pos.x,
		camera_target.y - camera_pos.y,
		camera_target.z - camera_pos.z);

	look_vec.normalize();
    }

    void AddObject(Object3D o) {
	objects++;

	o.Next = HeadObjectList;
	HeadObjectList = o;
    }

    void AddLight(Light3D l) {
	lights++;

	l.NextLight = HeadLight;
	HeadLight = l;

	AddObject(l);
    }

    GfxColor GetLight(Vector3D pos,
	    Vector3D normal,
	    int depth)
    {


	GfxColor c = new GfxColor(background);

	Light3D l = HeadLight;





	while (l != null)
	{
	    Ray3D shadow_ray = new Ray3D();

	    shadow_ray.pos = pos;
	    shadow_ray.min_dist = MAX_DIST;



	    shadow_ray.vec = Vector3D.Substract(l.origin, pos);
	    shadow_ray.vec.normalize();




	    shadow_ray.pos = new Vector3D(pos.x + (shadow_ray.vec.x * 2 * MIN_DIST),
		    pos.y + (shadow_ray.vec.y * 2 * MIN_DIST),
		    pos.z + (shadow_ray.vec.z * 2 * MIN_DIST));




	    if (Trace(shadow_ray, depth))
	    {
		double angle = Math.abs( Vector3D.DotProduct(shadow_ray.vec, normal) );
		if (angle > 1)
		    angle = 1;

		c.Add( shadow_ray.color ).DirectMultiply((float)angle);


	    }

	    l = l.NextLight;
	}

	return c.UpperClip();
    }

    void RotateZX(Vector3D axis,
	    double angle)
    {
	Object3D o = HeadObjectList;
	while (o != null)
	{
	    o.RotateZX(axis, angle);
	    o = o.Next;
	}
    }

    Object3D Shoot(Ray3D ray)
    {
	Object3D o = HeadObjectList;
	ray.closest = null;
	while (o != null)
	{
	    o.Intersect(ray);
	    o = o.Next;
	}



	return ray.closest;
    }

    boolean Trace(Ray3D ray, 
	    int depth)
    {
	depth++;
	if (depth > MAX_RAY_DEPTH)
	    return false;

	Object3D o = Shoot(ray);

	if (o == null)
	    return false;

	Vector3D pos = ray.Advance(ray.min_dist);

	ray.color = o.GetColor(pos,
		ray.vec,
		depth);
	return true;
    }


    GfxColor Refract(Vector3D normal,
	    Vector3D vec,
	    Vector3D pos,
	    int depth,
	    double factor)		   
    {
	Ray3D reflected_ray;
	double Normal_Component;

	double angle1 = Vector3D.DotProduct(vec, normal) * factor;

	Vector3D new_vec = Vector3D.Interpolate(normal,
		vec,
		angle1,
		1);
	new_vec.normalize();



	pos = new Vector3D(pos.x + (new_vec.x * 2 * MIN_DIST),
		pos.y + (new_vec.y * 2 * MIN_DIST),
		pos.z + (new_vec.z * 2 * MIN_DIST));

	reflected_ray = new Ray3D(pos,
		new_vec,
		new GfxColor(background));



	if (Trace (reflected_ray, depth))
	{

	    return reflected_ray.color.Multiply(2);
	}
	return background;
    }  

    GfxColor Reflect(Vector3D normal,
	    Vector3D vec,
	    Vector3D pos,
	    int depth)
    {
	return Refract(normal, vec, pos, depth, -2);
    }


    GfxColor GetPixelGfxColor(int px,
	    int py)
    { 
	Vector3D v = new Vector3D(px,
		py,
		cz * (180.0f/view_angle));
	v.normalize();

	double zy_angle = (double) (Math.PI - Math.atan2(look_vec.z, look_vec.y));
	double xy_angle = (double) (Math.PI - Math.atan2(look_vec.y, look_vec.x));

	v.RotateZY(zy_angle);
	v.RotateXY(xy_angle);

	Ray3D ray = new Ray3D(camera_pos,
		v,
		background);
	Trace(ray, 0);

	ray.color.checkRGB();
	return ray.color;
    }


    public VirtualScreen DivideAndConquer(int x, int y, int w, int h) {

	if(HeadLight == null) {
	    System.out.println("AAAA");
	}

	if(HeadObjectList == null) {
	    System.out.println("BBBBBB");
	}

	//		System.out.println("DIV: objlist = " + HeadObjectList.getString());

	if (w == 1 && h == 1) {
	    GfxColor c = GetPixelGfxColor(x - mx, y - my);

	    if (c == null) {
		System.out.println("Eek !");
		System.exit(99);
	    }

	    return new VirtualScreen( c );
	} else  {
	    int nw = w / 2;
	    int nh = h / 2;

	    if (nw == 0) {
		nw = 1;
	    }
	    if (nh == 0) {
		nh = 1;
	    }

	    VirtualScreen result = new VirtualScreen(w,h);

	    VirtualScreen sub1=null, sub2=null, sub3=null, sub4=null;

	    sub1 = /*SPAWN*/  DivideAndConquer(x, y, nw, nh);
	    sub2 = /*SPAWN*/  DivideAndConquer(x + nw, y, nw, nh);
	    sub3 = /*SPAWN*/  DivideAndConquer(x, y + nh, nw, nh);
	    sub4 = /*SPAWN*/  DivideAndConquer(x + nw, y + nh, nw, nh);
	    sync();

	    result.Set(0,   0, nw, nh, sub1);
	    result.Set(nw,  0, nw, nh, sub2);
	    result.Set(0,  nh, nw, nh, sub3);
	    result.Set(nw, nh, nw, nh, sub4);

	    return result;
	}      
    }
}
