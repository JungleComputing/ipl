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

    private HashMap<String, ASMClassInfo> javaClasses
            = new HashMap<String, ASMClassInfo>();

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
        return parseInputStream(new BufferedInputStream(new FileInputStream(fileName)), fileName);
    }

    @Override
    public ClassInfo parseInputStream(InputStream in, String fileName)
            throws IOException {
        ClassNode n = new ClassNode(Opcodes.ASM4);
        ClassReader r = new ClassReader(in);
        r.accept(n, 0);
        ASMRepository.addClass(n);
        return new ASMClassInfo(n);
    }

}
