import ibis.satin.so.*;

public interface BodiesInterface extends WriteMethodsInterface {

    public void updateBodies(double[] accs_x, double[] accs_y, double[] accs_z,
			     int iteration);

}
