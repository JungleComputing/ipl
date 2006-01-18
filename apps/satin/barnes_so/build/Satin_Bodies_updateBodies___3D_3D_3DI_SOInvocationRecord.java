import ibis.satin.*;

import ibis.satin.impl.*;

public final class Satin_Bodies_updateBodies___3D_3D_3DI_SOInvocationRecord extends SOInvocationRecord {
	double[] param0;
	double[] param1;
	double[] param2;
	int param3;

	public Satin_Bodies_updateBodies___3D_3D_3DI_SOInvocationRecord(String objectId, double[] param0, double[] param1, double[] param2, int param3) {
		super(objectId);
		this.param0 = param0;
		this.param1 = param1;
		this.param2 = param2;
		this.param3 = param3;
	}

	public void invoke(SharedObject object) {
		Bodies obj = (Bodies) object;
		try{
			obj.so_local_updateBodies(param0, param1, param2, param3);
		} catch (Throwable t) {
			/* exceptions will be only thrown at the originating node*/
		}
	}

}
