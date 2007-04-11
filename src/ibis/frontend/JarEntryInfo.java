/* $Id$ */

package ibis.frontend;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.zip.ZipOutputStream;

/**
 * Maintains information about the content of a jarfile entry.
 */
public class JarEntryInfo {

    private JarEntry       jarEntry;
    private byte[]         content;
    private JarInfo        jarInfo;
    private IbiscEntry     ibiscEntry;


    JarEntryInfo(JarEntry jarEntry, JarInfo jarInfo) throws IOException {
        this.jarEntry = jarEntry;
        this.jarInfo = jarInfo;
        this.content = getContent();
        this.ibiscEntry = getIbiscEntry();
    }

    JarEntryInfo(JarEntry jarEntry, JarInfo jarInfo, byte[] content) {
        this.jarEntry = jarEntry;
        this.jarInfo = jarInfo;
        this.content = content;
        this.ibiscEntry = getIbiscEntry();
    }

    JarEntryInfo(JarEntry jarEntry, JarInfo jarInfo, byte[] content, IbiscEntry ibiscEntry) {
        this.jarEntry = jarEntry;
        this.jarInfo = jarInfo;
        this.content = content;
        this.ibiscEntry = ibiscEntry;
    }
   
    private IbiscEntry getIbiscEntry() {
        String iname = jarEntry.getName();
        if (iname.endsWith(".class")) {
            try {
                ClassInfo cl = Ibisc.w.parseInputStream(getInputStream(),
                            iname);
                IbiscEntry entry = new IbiscEntry(cl, iname, jarInfo);
                Ibisc.allClasses.put(cl.getClassName(), entry);
                return entry;
            } catch(IOException e) {
                System.err.println("Ibisc: warning: could not read "
                        + "class " + iname + " from jar file "
                        + jarInfo.getName());
            }
        }
        return null;
    }
    
    private byte[] getContent() throws IOException {
        ByteArrayOutputStream Bos = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        InputStream in = jarInfo.getInputStream(jarEntry);
        int cnt;
        do {
            cnt = in.read(buf, 0, 16384);
            if (cnt >= 0) {
                Bos.write(buf, 0, cnt);
            }
        } while (cnt >= 0);
        buf = Bos.toByteArray();
        return buf;
    }

    void write(ZipOutputStream ozip) throws IOException {
        JarEntry je = new JarEntry(jarEntry.getName());
        byte[] extra = jarEntry.getExtra();
        if (extra != null) {
            je.setExtra(extra);
        }
        if (ibiscEntry != null && ibiscEntry.getModified()) {
            je.setTime(System.currentTimeMillis());
            content = ibiscEntry.getClassInfo().getBytes();
        }
        ozip.putNextEntry(je);
        ozip.write(content);
        ozip.closeEntry();
    }

    InputStream getInputStream() {
        return new ByteArrayInputStream(content);
    }



    /**
     * @return the jarEntry
     */
    public JarEntry getJarEntry() {
        return jarEntry;
    }

    /**
     * @return the jarInfo
     */
    public JarInfo getJarInfo() {
        return jarInfo;
    }
}
