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
package ibis.ipl.impl.stacking.lrmc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ibis.ipl.impl.stacking.lrmc.io.LrmcInputStream;
import ibis.ipl.impl.stacking.lrmc.util.Message;
import ibis.ipl.impl.stacking.lrmc.util.MessageCache;

public class InputStreams {

    private static final int DEFAULT_SIZE = 64;

    private static final Logger logger = LoggerFactory.getLogger(InputStreams.class);

    private LrmcInputStream[] inputStreams = new LrmcInputStream[DEFAULT_SIZE];

    private boolean[] hasData = new boolean[DEFAULT_SIZE];

    private boolean[] busy = new boolean[DEFAULT_SIZE];

    private int streamsWithData = 0;

    private int index = 0;

    private int last = -1;

    private boolean finish = false;

    private void add(LrmcInputStream is, int sender) {
        if (sender >= inputStreams.length) {
            resize(sender);
        }

        inputStreams[sender] = is;
        if (sender > last) {
            last = sender;
        }
    }

    public synchronized void terminate() {
        finish = true;
        for (int i = 0; i <= last; i++) {
            if (inputStreams[i] != null) {
                inputStreams[i].terminate();
            }
        }
        notifyAll();
    }

    private void resize(int minimumSize) {
        int newSize = hasData.length;

        while (newSize <= minimumSize) {
            newSize *= 2;
        }

        LrmcInputStream[] tmp1 = new LrmcInputStream[newSize];
        System.arraycopy(inputStreams, 0, tmp1, 0, inputStreams.length);
        inputStreams = tmp1;

        boolean[] tmp2 = new boolean[newSize];
        System.arraycopy(hasData, 0, tmp2, 0, hasData.length);
        hasData = tmp2;

        boolean[] tmp3 = new boolean[newSize];
        System.arraycopy(busy, 0, tmp3, 0, busy.length);
        busy = tmp3;
    }

    public synchronized LrmcInputStream get(int sender, MessageCache cache) {
        LrmcInputStream tmp = find(sender);
        if (tmp == null) {
            tmp = new LrmcInputStream(sender, cache);
            add(tmp, sender);
        }
        return tmp;
    }

    private LrmcInputStream find(int sender) {
        if (sender < 0 || sender > last) {
            return null;
        }

        return inputStreams[sender];
    }

    public synchronized void returnStream(LrmcInputStream is) {
        busy[is.getSource()] = false;
        if (is.haveData()) {
            if (logger.isDebugEnabled()) {
                logger.debug("return stream " + is.getSource() + ", still has data");
            }
            hasData(is);
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("return stream " + is.getSource() + ", no data left");
            }
        }
    }

    public synchronized boolean hasData(LrmcInputStream is, Message m) {
        if (is.addMessage(m)) {
            hasData(is);
            return true;
        }
        return false;
    }

    public synchronized void hasData(LrmcInputStream is) {
        int src = is.getSource();
        if (!hasData[src] && !busy[src]) {
            // Fix: Test before setting and incrementing counter (Ceriel)
            // Fix: Don't set hasData while it is busy. This may be incorrect
            // when we are still reading from the stream. We will see if
            // there is new data when we return the stream. (Ceriel)
            hasData[src] = true;
            if (logger.isDebugEnabled()) {
                logger.debug("Setting hasData for stream " + src);
                if (!is.haveData()) {
                    logger.debug("Set hasData but no data?", new Throwable());
                }
            }
            streamsWithData++;

            if (streamsWithData == 1) {
                notifyAll();
            }
        }
    }

    public synchronized LrmcInputStream getNextFilledStream() {

        while (!finish && streamsWithData == 0) {
            try {
                wait();
            } catch (Exception e) {
                // ignored
            }
        }
        if (finish) {
            return null;
        }

        final int size = inputStreams.length;

        for (int i = 1; i <= size; i++) {
            if (hasData[(index + i) % size]) {
                index = (index + i) % size;
                break;
            }
        }

        if (logger.isDebugEnabled() && !hasData[index]) {
            logger.debug("GetNextFilledStream returns !hasData stream" + ", streamsWithData = " + streamsWithData + ", index = " + index,
                    new Throwable());
        }

        hasData[index] = false;
        if (logger.isDebugEnabled()) {
            logger.debug("start read from stream " + index);
        }

        busy[index] = true;

        if (logger.isDebugEnabled() && !inputStreams[index].haveData()) {
            logger.debug("GetNextFilledStream returns empty stream" + ", streamsWithData = " + streamsWithData + ", index = " + index,
                    new Throwable());
        }

        streamsWithData--;

        return inputStreams[index];
    }
}
