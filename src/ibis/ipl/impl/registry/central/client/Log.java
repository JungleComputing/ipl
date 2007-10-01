package ibis.ipl.impl.registry.central.client;

import ibis.ipl.impl.registry.central.Event;
import ibis.util.ThreadPool;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

class Log implements Runnable {

    private static final Logger logger = Logger.getLogger(Log.class);

    private static class LogEntry {
        private long time;
        private String value;

        LogEntry(long time, String value) {
            this.time = time;
            this.value = value;
        }

        public String toString() {
            return time + " " + value;
        }
    }

    private final List<LogEntry> entries;
    private final long start;
    private final Pool pool;

    private String fileName;

    public Log(Pool pool) {
        entries = new ArrayList<LogEntry>();
        start = System.currentTimeMillis();
        this.pool = pool;
        fileName = null;

    }

    public synchronized void start(String fileName) {
        this.fileName = fileName;

        ThreadPool.createNew(this, "log save thread");
    }

    synchronized void log(Event event, int poolSize) {
        String message = null;

        switch (event.getType()) {
        case Event.JOIN:
        case Event.LEAVE:
        case Event.DIED:
            message = "POOLSIZE " + poolSize;
            break;
        case Event.SIGNAL:
            message = "SIGNAL " + event.getDescription();
            break;
        case Event.ELECT:
            message = "ELECTION " + event.getDescription() + " "
                    + event.getFirstIbis();
            break;
        case Event.UN_ELECT:
            message = "ELECTION " + event.getDescription() + " NULL";
            break;
        case Event.POOL_CLOSED:
            message = "POOL_CLOSED";
            break;
        }

        long time = System.currentTimeMillis() - start;
        entries.add(new LogEntry(time, message));
    }

    public synchronized void save() {
        if (fileName == null) {
            return;
        }
        try {
            File file = new File(fileName);

            if (file.exists()) {
                file.renameTo(new File(fileName + ".old"));
            }

            FileWriter out = new FileWriter(file);

            for (LogEntry entry : entries) {
                out.write(entry.toString() + "\n");
            }

            out.flush();
            out.close();
        } catch (IOException e) {
            logger.error("could not save log file", e);
        }
    }

    public synchronized void run() {

        while (!pool.isStopped()) {
            try {
                wait(60000);
            } catch (InterruptedException e) {
                // IGNORE
            }

            save();
        }
    }
}
