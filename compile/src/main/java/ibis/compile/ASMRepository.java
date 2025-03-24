/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ibis.compile;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

public class ASMRepository implements Opcodes {
    public static HashMap<String, ClassNode> classes = new HashMap<String, ClassNode>();

    public static void addClass(ClassNode cl) {
	classes.put(cl.name, cl);
    }

    public static ClassNode findClass(String name) throws ClassNotFoundException {
	ClassNode clazz = classes.get(name);
	if (clazz == null) {
	    String externalName = name;
	    if (name.contains("/")) {
		externalName = name.replaceAll("/", ".");
	    }
	    ClassReader cr;
	    try {
		cr = new ClassReader(externalName);
	    } catch (IOException e) {
		throw new ClassNotFoundException("Class not found: " + externalName, e);
	    }
	    clazz = new ClassNode(ASM4);
	    cr.accept(clazz, 0);
	    classes.put(name, clazz);
	}
	return clazz;
    }

    public static void dump(String fileName, ClassNode clazz) throws IOException {
	ClassWriter w = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
	clazz.accept(w);
	byte[] b = w.toByteArray();
	FileOutputStream o = new FileOutputStream(fileName);
	o.write(b);
	o.close();
    }

    public static void removeClass(String name) {
	if (name.contains(".")) {
	    name = name.replaceAll(".", "/");
	}
	classes.remove(name);
    }

    public static ClassNode parseClassFile(String fileName) throws IOException {
	return parseInputStream(new BufferedInputStream(new FileInputStream(fileName)), fileName);
    }

    public static ClassNode parseInputStream(InputStream in, String fileName) throws IOException {
	ClassNode n = new ClassNode(Opcodes.ASM4);
	ClassReader r = new ClassReader(in);
	r.accept(n, 0);
	classes.put(n.name, n);
	return n;
    }

    public static boolean instanceOf(ClassNode clazz, String type) throws ClassNotFoundException {
	ClassNode n = clazz;
	for (;;) {
	    if (n.name.equals(type)) {
		return true;
	    }
	    if (n.name.equals("java/lang/Object")) {
		break;
	    }
	    if (n.superName == null) {
		break;
	    }
	    n = findClass(n.superName);
	}
	return implementationOf(clazz, type);
    }

    public static ClassNode[] getSuperClasses(ClassNode clazz) throws ClassNotFoundException {
	ArrayList<ClassNode> supers = new ArrayList<ClassNode>();
	while (!clazz.name.equals("java/lang/Object") && clazz.superName != null) {
	    ClassReader r;
	    try {
		r = new ClassReader(clazz.superName);
	    } catch (IOException e) {
		throw new ClassNotFoundException("Could not find class " + clazz.superName, e);
	    }
	    clazz = new ClassNode(Opcodes.ASM4);
	    r.accept(clazz, 0);
	    classes.put(clazz.name, clazz);
	    supers.add(clazz);
	}
	return supers.toArray(new ClassNode[supers.size()]);
    }

    public static boolean implementationOf(ClassNode clazz,
            String type) throws ClassNotFoundException {
//    	System.out.println("implementationOf class: " + clazz.name + " type: " + type);
        List<String> interfaces = clazz.interfaces;
        for (String i : interfaces) { 
            if (i.equals(type)) {
                return true;
            } else {
                // the interface might extend the interface we are looking for
        	ClassNode inter = findClass(i);
        	if(inter != null) {
//        	    System.out.println("FOUND interface: " + i);
        	    if(implementationOf(inter, type)) {
//        		System.out.println("FOUND interface: " + i + ", IT IMPLEMENTS " + type);
        		return true;
        	    }
        	} else {
        	    System.err.println("interface NOT FOUND: " + i);
        	    return false;
        	}
            }
        }
        if (clazz.name.equals("java/lang/Object")) {
            return false;
        }
        if (clazz.superName == null) {
            return false;
        }
        ClassNode superClass = findClass(clazz.superName);
        return implementationOf(superClass, type);
    }

    public static void clearCache() {
	classes.clear();
    }
}
