package ibis.frontend.repmi;

import java.util.Vector;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import com.ibm.jikesbt.*;   
import ibis.util.BT_Analyzer;

class RepMIGenerator {
 
	public static BT_Class getReturnType(BT_Method m) { 
		return m.getSignature().returnType;
	}
	
	public static BT_ClassVector getParameterTypes(BT_Method m) { 
		return m.getSignature().types;
	}

	public static String containerType(BT_Class c) { 

		if (!c.equals( BT_Class.getVoid())) { 
			
			if (c.isPrimitive()) {
				
				if (c.equals( BT_Class.getByte())) { 
					return "Byte";
				} else if (c.equals( BT_Class.getChar())) { 
					return "Character";
				} else if (c.equals( BT_Class.getShort())) {
					return "Short";
				} else if (c.equals( BT_Class.getInt())) {
					return "Integer";
				} else if (c.equals( BT_Class.getLong())) {
					return "Long";
				} else if (c.equals( BT_Class.getFloat())) {
					return "Float";
				} else if (c.equals( BT_Class.getDouble())) {
					return "Double";
				} else if (c.equals( BT_Class.getBoolean())) {
					return "Boolean";
				} 					       
			}
			return "Object";
		} else { 
			return "Object";
		}
	} 

	public static String getType(BT_Class c) { 

		if (c.isArray()) {
			return getType(c.arrayType) + "[]";
		} else { 
			if (!c.equals( BT_Class.getVoid())) { 
				
				if (c.isPrimitive()) {
					
					if (c.equals( BT_Class.getByte())) { 
						return "byte";
					} else if (c.equals( BT_Class.getChar())) { 
						return "char";
					} else if (c.equals( BT_Class.getShort())) {
						return "short";
					} else if (c.equals( BT_Class.getInt())) {
						return "int";
					} else if (c.equals( BT_Class.getLong())) {
						return "long";
					} else if (c.equals( BT_Class.getFloat())) {
						return "float";
					} else if (c.equals( BT_Class.getDouble())) {
						return "double";
					} else if (c.equals( BT_Class.getBoolean())) {
						return "boolean";
					} 					       
				}
				return c.getName();
			} else { 
				return "void";
			}
		}
	} 
	
	public static String printType(BT_Class c) { 

		if (!c.equals( BT_Class.getVoid())) { 
			
			if (c.isPrimitive()) {
				
				if (c.equals( BT_Class.getByte())) { 
					return "Byte";
				} else if (c.equals( BT_Class.getChar())) { 
					return "Char";
				} else if (c.equals( BT_Class.getShort())) {
					return "Short";
				} else if (c.equals( BT_Class.getInt())) {
					return "Int";
				} else if (c.equals( BT_Class.getLong())) {
					return "Long";
				} else if (c.equals( BT_Class.getFloat())) {
					return "Float";
				} else if (c.equals( BT_Class.getDouble())) {
					return "Double";
				} else if (c.equals( BT_Class.getBoolean())) {
					return "Boolean";
				} 					       
			} else { 
				return "Object";
			} 		
		} 
		return null;
	} 
       
	public static String writeMessageType(String pre, String message, BT_Class c, String param) { 
		return (pre + message + ".write" + printType(c) + "(" + param + ");");
	} 
   
        public static String readMessageType(String pre, String dest, String message, BT_Class c) { 
		String temp = pre + dest + " = ";

		if (!c.isPrimitive()) {
			temp += "(" + getType(c) + ") ";
		}

		temp += message + ".read" + printType(c) + "();";

		return temp;
	} 
} 
