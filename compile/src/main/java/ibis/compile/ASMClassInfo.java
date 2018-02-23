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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

public class ASMClassInfo implements ClassInfo {
    
    private ClassNode n;
    
    public ASMClassInfo(ClassNode n) {
        this.n = n;
    }
    
    @Override
    public String getClassName() {
        return n.name.replaceAll("/", ".");
    }

    @Override
    public Object getClassObject() {
        return n;
    }
    

    void setClassObject(ClassNode n) {
        this.n = n;
    }

    @Override
    public void dump(String fileName) throws IOException {
        byte[] b = getBytes();
        FileOutputStream o = new FileOutputStream(fileName);
        o.write(b);
        o.close();
    }

    @Override
    public byte[] getBytes() {
        ClassWriter w = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        if (Ibisc.debug) {
            TraceClassVisitor tw = new TraceClassVisitor(new PrintWriter(System.out));
            CheckClassAdapter cw = new CheckClassAdapter(tw, false);
            n.accept(cw);
        }
        n.accept(w);
        return w.toByteArray();
    }

    @Override
    public boolean doVerify() {
        return true;
    }
}
