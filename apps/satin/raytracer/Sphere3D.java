
class Sphere3D extends Object3D implements java.io.Serializable 
{
    Vector3D origin;  
    double    radius, radius_sqr;


    Sphere3D(World world,
	    Vector3D center,
	    double rad,
	    Surface c)
    {
	super(world, c);

	origin = center;
	radius = rad;
	radius_sqr = rad * rad;

	surface.reflection = 0.5f;
    }


    public String toString()
    {
	return "Sphere3D [" + origin + ", radius=" + radius + "]";
    }

    void RotateZX(Vector3D axis,
	    double a)

    {
	Vector3D new_center = Vector3D.Substract(origin , axis);
	new_center.RotateZX(a);
	origin = Vector3D.Add(new_center, axis);
    }

    GfxColor GetColor(Vector3D pos,
	    Vector3D vec,
	    int depth)
    { 
	Vector3D normal = Vector3D.Substract(pos, origin);
	normal.normalize();

	Vector3D inv_normal = new Vector3D(normal).Invert();


	double angle = Math.abs(Vector3D.DotProduct(normal, vec) * surface.angle_dep);

	if (angle>1) 
	    angle = 1;

	GfxColor light = world.GetLight(pos, normal, depth);

	GfxColor c = GfxColor.Interpolate(surface.color.Multiply((float)angle).Multiply(light),
		world.background,
		surface.diffusity);

	GfxColor reflected_color = world.Reflect(normal,
		vec,
		pos,
		depth);

	GfxColor refracted_color = world.Refract(inv_normal,
		vec,
		pos,
		depth,
		surface.refraction);

	if (refracted_color == null)
	    refracted_color = world.background;

	if (reflected_color == null)	 
	    reflected_color = world.background;



	c.r = (c.r + 
		(refracted_color.r * surface.refraction) + 
		(reflected_color.r * surface.reflection)) / surface.absorbtion;

	c.g = (c.g + 
		(refracted_color.g * surface.refraction) + 
		(reflected_color.g * surface.reflection)) / surface.absorbtion;

	c.b = (c.b + 
		(refracted_color.b * surface.refraction) + 
		(reflected_color.b * surface.reflection)) / surface.absorbtion;

	c.DirectMultiply(1/(depth));

	c.UpperClip();


	return c; 
    }


    boolean Intersect(Ray3D ray)
    {
	double OCSquared, t_Closest_Approach, Half_Chord, t_Half_Chord_Squared;
	Vector3D Origin_To_Center;

	Origin_To_Center = Vector3D.Substract(origin, ray.pos);

	OCSquared          = Vector3D.DotProduct(Origin_To_Center, Origin_To_Center);
	t_Closest_Approach = Vector3D.DotProduct(Origin_To_Center, ray.vec);

	if ((OCSquared >= radius_sqr) && (t_Closest_Approach < World.MIN_DIST))
	{	  
	    return false;
	}

	t_Half_Chord_Squared = ( radius_sqr - OCSquared + (t_Closest_Approach * t_Closest_Approach) );      

	if (t_Half_Chord_Squared > World.MIN_DIST_SQR)
	{
	    Half_Chord = (double) Math.sqrt(t_Half_Chord_Squared);

	    double Depth1 = t_Closest_Approach - Half_Chord;
	    double Depth2 = t_Closest_Approach + Half_Chord;

	    if (Depth1 > Depth2)
	    {
		double temp = Depth2;
		Depth2 = temp;
		Depth1 = Depth2;
	    }



	    if (Depth1 > World.MIN_DIST && Depth1 < ray.min_dist)
	    {

		ray.min_dist = Depth1;
		ray.closest  = this;
		return true;
	    }

	    if (Depth2 > World.MIN_DIST && Depth2 < ray.min_dist)
	    {

		ray.min_dist = Depth2;
		ray.closest  = this;
		return true;	    
	    }
	}
	return false;
    }  
}









