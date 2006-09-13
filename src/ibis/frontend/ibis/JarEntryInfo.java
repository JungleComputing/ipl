/* $Id:$ */

package ibis.frontend.ibis;

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

    public JarEntry       jarEntry;
    public byte[]         content;
    public JarInfo        jarInfo;
    public IbiscEntry     ibiscEntry;

    JarEntryInfo(JarEntry jarEntry, JarInfo jarInfo) throws IOException {
        this.jarEntry = jarEntry;
        this.jarInfo = jarInfo;
        content = getContent();
    }

    JarEntryInfo(JarEntry jarEntry, JarInfo jarInfo, byte[] content) {
        this.jarEntry = jarEntry;
        this.jarInfo = jarInfo;
        this.content = content;
    }

    private byte[] getContent() throws IOException {
        ByteArrayOutputStream Bos = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        InputStream in = jarInfo.jarFile.getInputStream(jarEntry);
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
        if (ibiscEntry != null && ibiscEntry.modified) {
            je.setTime(System.currentTimeMillis());
            content = ibiscEntry.cl.getBytes();
        }
        ozip.putNextEntry(je);
        ozip.write(content);
        ozip.closeEntry();
    }

    InputStream getInputStream() {
        return new ByteArrayInputStream(content);
    }
}
