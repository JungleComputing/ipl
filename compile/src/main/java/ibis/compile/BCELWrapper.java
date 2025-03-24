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
/* $Id$ */

package ibis.compile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
/*
import org.apache.bcel.util.ClassPath;
import org.apache.bcel.util.SyntheticRepository;
*/

/**
 * BCEL implementation of the <code>ByteCodeWrapper</code> interface.
 */
public class BCELWrapper implements ByteCodeWrapper {

    private HashMap<String, BCELClassInfo> javaClasses = new HashMap<>();

    /**
     * Constructs a BCEL implementation object for the <code>ByteCodeWrapper</code>
     * interface.
     *
     * @param args list of classpath elements.
     */
    public BCELWrapper(List<String> args) {
        /*
         * String classPath = ClassPath.getClassPath(); String sep =
         * System.getProperty("path.separator"); for (String arg : args) { classPath =
         * classPath + sep + arg; } SyntheticRepository rep =
         * SyntheticRepository.getInstance( new ClassPath(classPath));
         * Repository.setRepository(rep);
         */
    }

    @Override
    public ClassInfo getInfo(Object o) {
        JavaClass cl = (JavaClass) o;
        String name = cl.getClassName();
        BCELClassInfo e = javaClasses.get(name);
        if (e == null) {
            e = new BCELClassInfo(cl);
            javaClasses.put(name, e);
        }
        e.setClassObject(cl);
        return e;
    }

    @Override
    public ClassInfo parseClassFile(String fileName) throws IOException {
        ClassParser parser = new ClassParser(fileName);
        JavaClass cl = parser.parse();
        Repository.addClass(cl);
        String name = cl.getClassName();
        BCELClassInfo e = javaClasses.get(name);
        if (e == null) {
            e = new BCELClassInfo(cl);
            javaClasses.put(name, e);
        }
        e.setClassObject(cl);
        Repository.addClass(cl);
        return e;
    }

    @Override
    public ClassInfo parseInputStream(InputStream in, String fileName) throws IOException {
        JavaClass cl = new ClassParser(in, fileName).parse();
        in.close();
        Repository.addClass(cl);
        String name = cl.getClassName();
        BCELClassInfo e = javaClasses.get(name);
        if (e == null) {
            e = new BCELClassInfo(cl);
            javaClasses.put(name, e);
        }
        e.setClassObject(cl);
        Repository.addClass(cl);
        return e;
    }
}
