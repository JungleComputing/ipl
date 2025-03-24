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
package ibis.ipl.benchmarks.registry;

import java.text.DateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private final IbisApplication[] apps;

    Main(int threads, boolean generateEvents, boolean fail) throws Exception {

        apps = new IbisApplication[threads];
        for (int i = 0; i < threads; i++) {
            logger.debug("starting thread " + i + " of " + threads);
            apps[i] = new IbisApplication(generateEvents, fail);
        }
    }

    void end() {
        for (IbisApplication app : apps) {
            app.end();
        }
    }

    void printStats() {
        int totalSeen = 0;
        for (IbisApplication app : apps) {
            totalSeen += app.nrOfIbisses();
        }
        double average = (double) totalSeen / (double) apps.length;

        String date = DateFormat.getTimeInstance().format(new Date(System.currentTimeMillis()));

        System.out.printf(date + " average seen members = %.2f\n", average, apps.length);
    }

    public static void main(String[] args) throws Exception {
        int threads = 1;
        boolean generateEvents = false;
        long start = System.currentTimeMillis();
        long runtime = Long.MAX_VALUE;
        long delay = 0;
        boolean fail = false;

        // int rank = new Integer(System.getProperty("rank", "0"));

        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("--threads")) {
                i++;
                threads = Integer.parseInt(args[i]);
            } else if (args[i].equalsIgnoreCase("--events")) {
                generateEvents = true;
            } else if (args[i].equalsIgnoreCase("--fail")) {
                fail = true;
            } else if (args[i].equalsIgnoreCase("--runtime")) {
                i++;
                runtime = Integer.valueOf(args[i]) * 1000;
            } else if (args[i].equalsIgnoreCase("--delay")) {
                i++;
                delay = Integer.valueOf(args[i]) * 1000;
            } else {
                System.err.println("unknown option: " + args[i]);
                System.exit(1);
            }
        }

        // delay specified time
        if (delay > 0) {
            Thread.sleep(delay);
        }

        // create ibisses
        new Main(threads, generateEvents, fail);

        // sleep for specified runtime
        long sleep = runtime - (System.currentTimeMillis() - start);
        System.err.println("Benchmark app sleeping : " + sleep);
        if (sleep > 0) {
            Thread.sleep(sleep);
        }

        // app stopped automatically
    }

}
