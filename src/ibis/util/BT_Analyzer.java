package ibis.util;

import com.ibm.jikesbt.*;   
import java.util.*;

public class BT_Analyzer { 
        boolean verbose;

	public BT_Class specialInterface;
	public BT_Class subject;

	public String classname;
	public String packagename;	     

	public Vector specialInterfaces;
	public Vector specialMethods;
	public Vector subjectSpecialMethods;

        public BT_Analyzer(BT_Class subject, BT_Class specialInterface, boolean verbose) { 
		this.subject          = subject;
		this.specialInterface = specialInterface;
		this.verbose          = verbose;
		
		if (verbose) { 
			System.out.println("BT_Analyzer looking for " + specialInterface.fullName() + " in " + subject.fullName());
		}
	}

	public boolean isSpecial(BT_Method m1) {
                if(specialMethods == null) {
                        return false;
                }

                for (int i=0;i<specialMethods.size();i++) { 
			BT_Method m2 = (BT_Method) specialMethods.get(i);
                        if(m1.getSignature().equals(m2.getSignature()) && m1.getName().equals(m2.getName())) {
                                return true;
                        }
                }
                return false;
	}
	
	private boolean compareMethods(BT_Method m1, BT_Method m2) {
		return m1.sigEquals(m2);
	} 
       
	private void addSpecialMethod(Vector specialMethods, BT_Method m) { 
		for (int i=0;i<specialMethods.size();i++) { 
			if (compareMethods(m, (BT_Method) specialMethods.get(i))) { 
				// it is already in the vector
				return;
			}
		}

		specialMethods.addElement(m);
	} 

	private void findSpecialMethods(BT_Class specialInterface, Vector specialMethods) { 
		BT_MethodVector methods = specialInterface.getMethods();

		for (int i=0;i<methods.size();i++) { 
			addSpecialMethod(specialMethods, methods.elementAt(i));
		}		
	}

	Vector findSpecialMethods(Vector specialInterfaces) { 
		Vector specialMethods = new Vector();

		for (int i=0;i<specialInterfaces.size();i++) { 
			findSpecialMethods((BT_Class) specialInterfaces.get(i), specialMethods);
		} 
		
		return specialMethods;
	} 

	private void findSubjectSpecialMethod(BT_MethodVector subject, BT_Method special, Vector dest) {

		for (int i=0;i<subject.size();i++) { 

			BT_Method temp = (BT_Method) subject.elementAt(i);

			if (compareMethods(special, temp)) { 
//				System.out.println(temp.fullName() + " equals " + special.fullName());
				dest.add(temp);
			}
		} 
	}
 
	Vector findSubjectSpecialMethods() { 
		Vector temp = new Vector();
		BT_MethodVector v = subject.getMethods();

		for (int i=0;i<specialMethods.size();i++) { 
			findSubjectSpecialMethod(v, (BT_Method) specialMethods.get(i), temp);
		} 
		
		return temp;
	} 


	boolean findSpecialInterfaces(BT_Class inter, Vector specialInterfaces) {
		boolean result = false;

		if (inter.equals(specialInterface)) {
			result = true;
		} else {
			BT_ClassVector interfaces = inter.getParents();
			
			for (int i=0;i<interfaces.size();i++) { 
				if(interfaces.elementAt(i).isInterface) {
					result |= findSpecialInterfaces(interfaces.elementAt(i), specialInterfaces);
				}
			}
		} 

		if (result) { 
			if (!specialInterfaces.contains(inter)) { 
				specialInterfaces.addElement(inter);
			}
		} 

		return result;
	}
	
	Vector findSpecialInterfaces() { 
		Vector specialInterfaces = new Vector();

		BT_ClassVector interfaces = subject.getParents();
		
		for (int i=0;i<interfaces.size();i++) { 
			if(interfaces.elementAt(i).isInterface) {
				findSpecialInterfaces(interfaces.elementAt(i), specialInterfaces);
			}
		}

		return specialInterfaces;
	}

	public void start() {

		String temp = subject.getName();
		StringTokenizer s = new StringTokenizer(temp, ".");

		int tokens = s.countTokens();

		if (tokens > 1) { 
			packagename = s.nextToken();

			for (int i=1;i<tokens-1;i++) { 
				packagename += "." + s.nextToken();
			}
		} 	

		classname = s.nextToken();	

		specialInterfaces = findSpecialInterfaces();
	
		if (specialInterfaces.size() == 0) { 
			if (verbose) { 
				System.out.println("Class " + subject.getName() + " does not implement " + specialInterface.getName());
			} 

			return;
		} 
			
		if (verbose) { 
			System.out.println(specialInterfaces.size() + " special interfaces found in " + subject.getName());

			for (int i=0;i<specialInterfaces.size();i++) { 
				System.out.println("\t" + ((BT_Class) specialInterfaces.elementAt(i)).getName());
			} 			
		} 

	        specialMethods = findSpecialMethods(specialInterfaces);

		if (verbose) { 
			System.out.println(specialMethods.size() + " special methods found in " + subject.getName());

			for (int i=0;i<specialMethods.size();i++) { 
				System.out.println("\t" + ((BT_Method) specialMethods.get(i)).toString());
			} 			
		} 

		subjectSpecialMethods = findSubjectSpecialMethods();
	} 
}
