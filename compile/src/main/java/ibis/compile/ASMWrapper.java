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
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

public class ASMWrapper implements ByteCodeWrapper {

    private HashMap<String, ASMClassInfo> javaClasses = new HashMap<>();

    public ASMWrapper(List<String> args) {
        // nothing for now.
    }

    @Override
    public ClassInfo getInfo(Object cl) {
        ClassNode n = (ClassNode) cl;
        String name = n.name.replaceAll("/", ".");
        ASMClassInfo e = javaClasses.get(name);
        if (e == null) {
            e = new ASMClassInfo(n);
            javaClasses.put(name, e);
        }
        e.setClassObject(n);
        return e;
    }

    @Override
    public ClassInfo parseClassFile(String fileName) throws IOException {
        InputStream in = new BufferedInputStream(new FileInputStream(fileName));
        ClassInfo result = parseInputStream(in, fileName);
        try {
            in.close();
        } catch (Throwable e) {
            // ignore
        }
        return result;
    }

    @Override
    public ClassInfo parseInputStream(InputStream in, String fileName) throws IOException {
        ClassNode n = new ClassNode(Opcodes.ASM4);
        ClassReader r = new ClassReader(in);
        r.accept(n, 0);
        ASMRepository.addClass(n);
        return new ASMClassInfo(n);
    }

}
