

class TrianglePatch3D extends Triangle3D
{
    Vector3D n1,n2,n3;

    TrianglePatch3D(World world,
	    Vector3D p1,
	    Vector3D n1,
	    Vector3D p2,
	    Vector3D n2,
	    Vector3D p3,
	    Vector3D n3,
	    Surface color)
    {
	super(world,
		p1,
		p2,
		p3,
		color);

	this.n1 = n1;
	this.n2 = n2;
	this.n3 = n3;
    }
}






