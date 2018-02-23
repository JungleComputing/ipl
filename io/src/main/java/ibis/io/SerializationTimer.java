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

package ibis.io;

import ibis.util.Timer;

import java.util.Vector;

public class SerializationTimer {

    private static Vector<SerializationTimer> timerList
    = new Vector<SerializationTimer>();

    static {
        Runtime.getRuntime().addShutdownHook(
                new Thread("SerializationStreams ShutdownHook") {
                    public void run() {
                        printAllTimers();
                    }
                });
    }

    private final String name;

    private int starts;

    private final Timer timer;

    private final Timer suspend;

    public SerializationTimer(String name) {
        this.name = name;
        suspend = Timer.createTimer();
        timer = Timer.createTimer();
        synchronized (SerializationTimer.class) {
            timerList.add(this);
        }
    }

    public void start() {
        if (starts++ == 0) {
            timer.start();
        }
    }

    public void stop() {
        if (--starts == 0) {
            timer.stop();
        }
    }

    public void suspend() {
        if (starts > 0) {
            timer.stop();
            suspend.start();
        }
    }

    public void resume() {
        if (starts > 0) {
            suspend.stop();
            timer.start();
        }
    }

    public void reset() {
        starts = 0;
        timer.reset();
        suspend.reset();
    }

    public void report(java.io.PrintStream s) {
        if (timer.nrTimes() > 0) {
            s.println("Timer \"" + name + "\" " + timer.totalTime() + " ("
                    + timer.nrTimes() + ");" + " suspend "
                    + suspend.totalTime() + " (" + suspend.nrTimes() + ")");
        }
    }

    public void report() {
        report(System.out);
    }

    public static void resetAllTimers() {
        synchronized (SerializationTimer.class) {
            for (SerializationTimer t : timerList) {
                t.reset();
            }
        }
    }

    public static void printAllTimers() {
        synchronized (SerializationTimer.class) {
            for (SerializationTimer t : timerList) {
                t.report();
            }
        }
    }
}
