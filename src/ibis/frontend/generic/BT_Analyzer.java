package ibis.frontend.generic;

import java.util.Vector;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

public class BT_Analyzer { 
        boolean verbose;

	public JavaClass specialInterface;
	public JavaClass subject;

	public String classname;
	public String packagename;	     

	public Vector specialInterfaces;
	public Vector specialMethods;
	public Vector subjectSpecialMethods;

        public BT_Analyzer(JavaClass subject, JavaClass specialInterface, boolean verbose) { 
		this.subject          = subject;
		this.specialInterface = specialInterface;
		this.verbose          = verbose;
		
		if (verbose) { 
			System.out.println("BT_Analyzer looking for " + specialInterface.getClassName() + " in " + subject.getClassName());
		}
	}

	private boolean compareMethods(Method m1, Method m2) {
		return m1.getSignature().equals(m2.getSignature()) && m1.getName().equals(m2.getName());
	} 
       
	public boolean isSpecial(Method m1) {
                if (specialMethods == null) {
                        return false;
                }

                for (int i=0;i<specialMethods.size();i++) { 
			Method m2 = (Method) specialMethods.get(i);
                        if (compareMethods(m1, m2)) {
                                return true;
                        }
                }

                return false;
	}
	
	private void addSpecialMethod(Vector sm, Method m) { 
		for (int i=0;i<sm.size();i++) { 
			if (compareMethods(m, (Method) sm.get(i))) { 
				// it is already in the vector
				return;
			}
		}

		sm.addElement(m);
	} 

	private void findSpecialMethods(JavaClass si, Vector sm) { 
		Method[] methods = si.getMethods();

		for (int i=0;i<methods.length;i++) { 
		    if (! methods[i].getName().equals("<clinit>")) {
			addSpecialMethod(sm, methods[i]);
		    }
		}		
	}

	Vector findSpecialMethods(Vector si) { 
		Vector mi = new Vector();

		for (int i=0;i<si.size();i++) { 
			findSpecialMethods((JavaClass) si.get(i), mi);
		} 
		
		return mi;
	} 

	private void findSubjectSpecialMethod(Method[] sub, Method special, Vector dest) {

		for (int i=0;i<sub.length;i++) { 

			Method temp = sub[i];

			if (compareMethods(special, temp)) { 
//				System.out.println(temp.fullName() + " equals " + special.fullName());
				dest.add(temp);
			}
		} 
	}
 
	Vector findSubjectSpecialMethods() { 
		Vector temp = new Vector();
		Method v[] = subject.getMethods();

		for (int i=0;i<specialMethods.size();i++) { 
			findSubjectSpecialMethod(v, (Method) specialMethods.get(i), temp);
		} 
		
		return temp;
	} 


	boolean findSpecialInterfaces(String inter, Vector si) {
		boolean result = false;
		JavaClass interf = Repository.lookupClass(inter);

		if(interf == null) {
		    System.err.println("interface " + inter + " not found.");
		    System.exit(1);
		}

		if (inter.equals(specialInterface.getClassName())) {
			result = true;
		} else {
			String[] interfaces = interf.getInterfaceNames();
			
			for (int i=0;i<interfaces.length;i++) { 
				result |= findSpecialInterfaces(interfaces[i], si);
			}
		} 

		if (result) { 
			if (!si.contains(inter)) { 
				si.addElement(interf);
			}
		} 

		return result;
	}

	Vector findSpecialInterfaces() { 
		Vector si = new Vector();

		if (! subject.isClass()) {
		    findSpecialInterfaces(subject.getClassName(), si);
		}
		else {
		    String[] interfaces = subject.getInterfaceNames();
		
		    for (int i=0;i<interfaces.length;i++) { 
			    if (verbose) {
				System.out.println(subject.getClassName() + " implements " + interfaces[i]);
			    }
			    findSpecialInterfaces(interfaces[i], si);
		    }
		}

		return si;
	}

	public void start() {

		String temp = subject.getClassName();
		packagename = subject.getPackageName();
		classname = temp.substring(temp.lastIndexOf('.')+1);

		specialInterfaces = findSpecialInterfaces();
	
		if (specialInterfaces.size() == 0) { 
			if (verbose) { 
				System.out.println("Class " + subject.getClassName() + " does not implement " + specialInterface.getClassName());
			} 

			return;
		} 
			
		if (verbose) { 
			System.out.println(specialInterfaces.size() + " special interfaces found in " + subject.getClassName());

			for (int i=0;i<specialInterfaces.size();i++) { 
				System.out.println("\t" + ((JavaClass) specialInterfaces.elementAt(i)).getClassName());
			} 			
		} 

	        specialMethods = findSpecialMethods(specialInterfaces);

		if (verbose) { 
			System.out.println(specialMethods.size() + " special methods found in " + subject.getClassName());

			for (int i=0;i<specialMethods.size();i++) { 
				System.out.println("\t" + ((Method) specialMethods.get(i)).toString());
			} 			
		} 

		subjectSpecialMethods = findSubjectSpecialMethods();
	} 
}
