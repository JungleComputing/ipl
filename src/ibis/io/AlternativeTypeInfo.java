package ibis.io;

import java.lang.reflect.*;
import java.security.*;
import java.util.*;

final class AlternativeTypeInfo {

	private static HashMap alternativeTypes = new HashMap();
	private static Class javaSerializableClass;

	Field [] serializable_fields;
	int double_count;
	int long_count;
	int float_count;
	int int_count;
	int short_count;
	int char_count;
	int byte_count;
	int boolean_count;
	int reference_count;

	boolean superSerializable;	
	AlternativeTypeInfo alternativeSuperInfo;

	boolean hasRWExternal;
	boolean hasRWObject;

	// This is needed for the private field access hack.
	private Field temporary_field;

	static { 
		try { 
			javaSerializableClass = Class.forName("java.io.Serializable");
		} catch (Exception e) { 
			System.err.println("OOPS: cannot load class java.io.Serializable : " + e);
			e.printStackTrace();
			System.exit(1);
		} 
	} 

	public static synchronized AlternativeTypeInfo getAlternativeTypeInfo(Class type) { 
		AlternativeTypeInfo t = (AlternativeTypeInfo) alternativeTypes.get(type);

		if (t == null) { 
			t = new AlternativeTypeInfo(type);
			alternativeTypes.put(type, t);
		} 
		
		return t;
	}

	private static void insert(Field [] array, int used, Field to_insert) {
		
		String name = to_insert.getName();

		for (int i=0;i<used;i++) { 
			int result = name.compareTo(array[i].getName());

			if (result < 0) { 
				// must insert in the middle it, so move the rest
				for (int j=used;j>i;j--) { 
					array[j] = array[j-1];
				} 
				array[i] = to_insert;
				return;
			} 
		} 
		// must insert at the end
		array[used] = to_insert;
	} 

	AlternativeTypeInfo(Class clazz) { 

		try {								
			/*
			  Here we figure out what field the type contains, and which fields 
			  we should write. We must also sort them by type and name to ensure
			  that we read them correctly on the other side. We cache all of this 
			  so we only do it once for each type.

			  TODO: Add check to see if object has read/writeObject or replace methods 
			*/
						
			Field [] fields = clazz.getDeclaredFields(); 

			// Create the datastructures to cache the fields we need. Since we don't know the 
			// size yet, we create large enough arrays, wich will later be replaced;
			Field [] double_fields    = new Field[fields.length]; 
			Field [] long_fields      = new Field[fields.length]; 
			Field [] float_fields     = new Field[fields.length]; 
			Field [] int_fields       = new Field[fields.length]; 
			Field [] short_fields     = new Field[fields.length]; 
			Field [] char_fields      = new Field[fields.length]; 
			Field [] byte_fields      = new Field[fields.length]; 
			Field [] boolean_fields   = new Field[fields.length]; 
			Field [] reference_fields = new Field[fields.length]; 

			// Now count and store all the difference field types (only the ones that we should write!).
			// Note that we store them into the array sorted by name !
			for (int i=0;i<fields.length;i++) { 

				Field field = fields[i];
				int modifiers = field.getModifiers();
				
				if ((modifiers & (Modifier.STATIC | Modifier.TRANSIENT)) == 0) { 
					
					Class field_type = field.getType();
					
					// This part is a bit scary. We basically switch of the Java field access checks
					// so we are allowed to read private fields ....

					if (!field.isAccessible()) { 					
						temporary_field = field;
						AccessController.doPrivileged(new PrivilegedAction() {
								public Object run() {
									temporary_field.setAccessible(true);
									return null;
								} 
							});
					}
					
					if (field_type.isPrimitive()) {
						
						if (field_type == Boolean.TYPE) { 
							insert(boolean_fields, boolean_count, field);
							boolean_count++;							
						} else if (field_type == Character.TYPE) { 
							insert(char_fields, char_count, field);
							char_count++;									
						} else if (field_type == Byte.TYPE) { 
							insert(byte_fields, byte_count, field);
							byte_count++;
						} else if (field_type == Short.TYPE) { 
							insert(short_fields, short_count, field);
							short_count++;
						} else if (field_type == Integer.TYPE) { 
							insert(int_fields, int_count, field);
							int_count++;
						} else if (field_type == Long.TYPE) { 
							insert(long_fields, long_count, field);
							long_count++;
						} else if (field_type == Float.TYPE) { 
							insert(float_fields, float_count, field);
							float_count++;
						} else if (field_type == Double.TYPE) { 
							insert(double_fields, double_count, field);
							double_count++;
						} 
					} else { 
						insert(reference_fields, reference_count, field);
						reference_count++;
					}
				}
			}
				
			// Now resize the datastructures.
			int size = double_count + long_count + float_count + int_count + short_count + char_count + byte_count + boolean_count + reference_count;
			int index = 0;				

			if (size > 0) { 
				serializable_fields = new Field[size];
				
				System.arraycopy(double_fields, 0, serializable_fields, index, double_count);
				index += double_count;

				System.arraycopy(long_fields, 0, serializable_fields, index,long_count);
				index += long_count;

				System.arraycopy(float_fields, 0, serializable_fields, index, float_count);
				index += float_count;

				System.arraycopy(int_fields, 0, serializable_fields, index, int_count);
				index += int_count;

				System.arraycopy(short_fields, 0, serializable_fields, index, short_count);
				index += short_count;

				System.arraycopy(char_fields, 0, serializable_fields, index, char_count);
				index += char_count;

				System.arraycopy(byte_fields, 0, serializable_fields, index, byte_count);
				index += byte_count;

				System.arraycopy(boolean_fields, 0, serializable_fields, index, boolean_count);
				index += boolean_count;

				System.arraycopy(reference_fields, 0, serializable_fields, index, reference_count);
			} else { 
				serializable_fields = null;
			}

			/* see if the supertype is serializable */			
			Class superClass = clazz.getSuperclass();

			if (superClass != null) { 
				if (javaSerializableClass.isAssignableFrom(superClass)) { 
					superSerializable = true;
					alternativeSuperInfo = getAlternativeTypeInfo(superClass);
				} else { 
					superSerializable = false;
				}								
			} 

/*
			System.err.println("Class " + clazz.getName() + " contains " + size + " serializable fields :");
			
			int temp = 0;
			for (int i=0;i<double_count;i++)    System.err.println("double " + serializable_fields[temp++].getName());
			for (int i=0;i<long_count;i++)      System.err.println("long " + serializable_fields[temp++].getName());
			for (int i=0;i<float_count;i++)     System.err.println("float " + serializable_fields[temp++].getName());
			for (int i=0;i<int_count;i++)       System.err.println("int " + serializable_fields[temp++].getName());
			for (int i=0;i<short_count;i++)     System.err.println("short " + serializable_fields[temp++].getName());
			for (int i=0;i<char_count;i++)      System.err.println("char " + serializable_fields[temp++].getName());
			for (int i=0;i<boolean_count;i++)   System.err.println("boolean " + serializable_fields[temp++].getName());
			for (int i=0;i<reference_count;i++) System.err.println("reference " + serializable_fields[temp++].getName());
*/
		} catch (Exception e) {
			System.err.println("Cannot initialize serialization info for " + clazz.getName() +" : " + e);
			e.printStackTrace();
			System.exit(1);
		} 			 
	} 
}
