package ibis.frontend.ibis;

import com.ibm.jikesbt.*;
import java.util.*;

class IbiscFactory extends BT_Factory {

	String target;
	Vector loadList = new Vector();

	IbiscFactory(String target) {
		this.target = target;
	}

	// override method in BT_Factory to avoid debug prints...
	public void noteClassLoaded(BT_Class bt_class, String s) {}

	public boolean isProjectClass(String className, Object file) { 
//		if(className.startsWith("Satin_")) return true;

//		return className.equals(target);

//		return false;

//		if(className.equals("ibis.satin.SatinObject")) return true;
//		if(className.equals("ibis.satin.Spawnable")) return true;

//		if(className.equals("ibis.satin.SpawnCounter")) return true;
//		if(className.equals("ibis.satin.ReturnRecord")) return true;
//		if(className.equals("ibis.satin.InvocationRecord")) return true;



		if (className.startsWith("ibis.")) return false;
		if (className.startsWith("java.")) return false;
		if (className.startsWith("sun.")) return false;
		if (className.startsWith("ibm.")) return false;
		if (className.startsWith("com.ibm.jikesbt.")) return false;
		//		System.out.println("Loading " + className);

		loadList.add(className);

		return true;
	}
}
