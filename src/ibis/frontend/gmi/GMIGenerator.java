package ibis.frontend.group;

import java.util.Vector;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import ibis.util.Analyzer;

class GMIGenerator { 

	public static String getInitedLocal(Class c, String name) { 

		String result = null;

		if (!c.equals(Void.TYPE)) { 			
			if (c.isPrimitive()) {				
				if (c.equals(Byte.TYPE)) { 
					result = "byte " + name + " = 0";
				} else if (c.equals(Character.TYPE)) { 
					result = "char " + name + " = 0";
				} else if (c.equals(Short.TYPE)) {
					result = "short " + name + " = 0";
				} else if (c.equals(Integer.TYPE)) {
					result = "int " + name + " = 0";
				} else if (c.equals(Long.TYPE)) {
					result = "long " + name + " = 0";
				} else if (c.equals(Float.TYPE)) {
					result = "float " + name + " = 0.0";
				} else if (c.equals(Double.TYPE)) {
					result = "double " + name + " = 0.0";
				} else if (c.equals(Boolean.TYPE)) {
					result = "boolean " + name + " = false";
				} 					       
			} else { 
				result = getType(c) + " " + name + " = null";
			}
		}

		return result;
	}

	public static String combineType(Class c) { 

		if (!c.equals(Void.TYPE)) { 
			
			if (c.isPrimitive()) {
				
				if (c.equals(Byte.TYPE)) { 
					return "COMBINE_BYTE";
				} else if (c.equals(Character.TYPE)) { 
					return "COMBINE_CHAR";
				} else if (c.equals(Short.TYPE)) {
					return "COMBINE_SHORT";
				} else if (c.equals(Integer.TYPE)) {
					return "COMBINE_INT";
				} else if (c.equals(Long.TYPE)) {
					return "COMNBINE_LONG";
				} else if (c.equals(Float.TYPE)) {
					return "COMBINE_FLOAT";
				} else if (c.equals(Double.TYPE)) {
					return "COMBINE_DOUBLE";
				} else if (c.equals(Boolean.TYPE)) {
					return "COMBINE_BOOLEAN";
				} 					       
			}
			return "COMBINE_OBJECT";
		} else { 
			return "COMBINE_VOID";
		}
	} 

	public static String containerType(Class c) { 

		if (!c.equals(Void.TYPE)) { 
			
			if (c.isPrimitive()) {
				
				if (c.equals(Byte.TYPE)) { 
					return "Byte";
				} else if (c.equals(Character.TYPE)) { 
					return "Character";
				} else if (c.equals(Short.TYPE)) {
					return "Short";
				} else if (c.equals(Integer.TYPE)) {
					return "Integer";
				} else if (c.equals(Long.TYPE)) {
					return "Long";
				} else if (c.equals(Float.TYPE)) {
					return "Float";
				} else if (c.equals(Double.TYPE)) {
					return "Double";
				} else if (c.equals(Boolean.TYPE)) {
					return "Boolean";
				} 					       
			}
			return "Object";
		} else { 
			return "Object";
		}
	} 

	public static String getType(Class c) { 

		if (c.isArray()) {
			return getType(c.getComponentType()) + "[]";
		} else { 
			if (!c.equals(Void.TYPE)) { 
				
				if (c.isPrimitive()) {
					
					if (c.equals(Byte.TYPE)) { 
						return "byte";
					} else if (c.equals(Character.TYPE)) { 
						return "char";
					} else if (c.equals(Short.TYPE)) {
						return "short";
					} else if (c.equals(Integer.TYPE)) {
						return "int";
					} else if (c.equals(Long.TYPE)) {
						return "long";
					} else if (c.equals(Float.TYPE)) {
						return "float";
					} else if (c.equals(Double.TYPE)) {
						return "double";
					} else if (c.equals(Boolean.TYPE)) {
						return "boolean";
					} 					       
				}
				return c.getName();
			} else { 
				return "void";
			}
		}
	} 
	
	public static String printType(Class c) { 

		if (!c.equals(Void.TYPE)) { 
			
			if (c.isPrimitive()) {
				
				if (c.equals(Byte.TYPE)) { 
					return "Byte";
				} else if (c.equals(Character.TYPE)) { 
					return "Char";
				} else if (c.equals(Short.TYPE)) {
					return "Short";
				} else if (c.equals(Integer.TYPE)) {
					return "Int";
				} else if (c.equals(Long.TYPE)) {
					return "Long";
				} else if (c.equals(Float.TYPE)) {
					return "Float";
				} else if (c.equals(Double.TYPE)) {
					return "Double";
				} else if (c.equals(Boolean.TYPE)) {
					return "Boolean";
				} 					       
			} else { 
				return "Object";
			} 		
		} 
		return null;
	} 
       
	public static String writeMessageType(String pre, String message, Class c, String param) { 
		return (pre + message + ".write" + printType(c) + "(" + param + ");");
	} 
   
        public static String readMessageType(String pre, String dest, String message, Class c, boolean cast) { 
		String temp = pre + dest + " = ";

		if (!c.isPrimitive() && cast) {
			temp += "(" + getType(c) + ") ";
		}

		temp += message + ".read" + printType(c) + "();";

		return temp;
	} 
} 
