package ibis.ipl.support;

import java.io.IOException;
import java.io.InputStream;

public final class CountInputStream extends InputStream {

    private final InputStream in;
    private int count;

    public CountInputStream(InputStream in) {
        this.in = in;
        count = 0;
    }

    public int getCount() {
        return count;
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        in.mark(readlimit);
    }

    @Override
    public boolean markSupported() {
        return in.markSupported();
    }

    @Override
    public int read() throws IOException {
        count++;
        return in.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int result = in.read(b, off, len);

        count += result;

        return result;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int result = in.read(b);

        count += result;

        return result;
    }

    @Override
    public synchronized void reset() throws IOException {
        in.reset();
    }

    @Override
    public long skip(long n) throws IOException {
        long result = in.skip(n);

        count += result;

        return result;
    }

}
