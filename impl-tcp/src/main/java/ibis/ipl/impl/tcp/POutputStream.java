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
package ibis.ipl.impl.tcp;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import ibis.io.Conversion;

public class POutputStream extends OutputStream {

    private Conversion conversion = Conversion.loadConversion(false);
    private OutputStream[] streams;
    private int currentStream;
    private byte[] tmp = new byte[1];
    private byte[] tmpInt = new byte[4];

    public POutputStream(Socket[] sockets) throws IOException {
        streams = new OutputStream[sockets.length];
        for (int i = 0; i < sockets.length; i++) {
            streams[i] = sockets[i].getOutputStream();
        }
    }

    @Override
    public void write(int v) throws IOException {
        tmp[0] = (byte) v;
        this.write(tmp);
    }

    @Override
    public void write(byte[] b) throws IOException {
        this.write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        conversion.int2byte(len, tmpInt, 0);

        streams[currentStream].write(tmpInt);
        streams[currentStream].write(b, off, len);

        currentStream = (currentStream + 1) % streams.length;
    }

    @Override
    public void flush() {
        // no flush needed
    }

    @Override
    public void close() throws IOException {
        if (streams != null) {
            try {
                for (OutputStream o : streams) {
                    o.close();
                }
            } finally {
                streams = null;
            }
        }
    }
}
