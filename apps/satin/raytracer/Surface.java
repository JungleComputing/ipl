

class Surface implements java.io.Serializable 
{
    GfxColor color    = new GfxColor(1,1,1);
    float reflection  = 0.5f;
    float specular    = 0.5f;
    float shine       = 0.5f;
    float refraction  = 0.5f;
    float diffusity   = 0.9f;
    float T           = 0.0f;

    float absorbtion = 0.2f;

    float angle_dep  = 0.6f;  

    Surface copy()
    {
	Surface s = new Surface();

	s.color    = color;
	s.reflection  = reflection;
	s.specular    = specular;
	s.shine       = shine;
	s.refraction  = refraction;
	s.diffusity   = diffusity;
	s.T           = T;


	return s;
    }
}
