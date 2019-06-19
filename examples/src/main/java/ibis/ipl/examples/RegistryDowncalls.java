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
package ibis.ipl.examples;

import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;

/**
 * This program shows how to handle events from the registry using downcalls. It
 * will run for 30 seconds, then stop. You can start as many instances of this
 * application as you like.
 */

public class RegistryDowncalls {

    IbisCapabilities ibisCapabilities = new IbisCapabilities(
            IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED);

    private void run() throws Exception {
        // Create an ibis instance, pass "null" as the event handler, enabling
        // downcalls
        Ibis ibis = IbisFactory.createIbis(ibisCapabilities, null);

        // poll the registry once every second for new events
        for (int i = 0; i < 30; i++) {

            // poll for new ibises
            IbisIdentifier[] joinedIbises = ibis.registry().joinedIbises();
            for (IbisIdentifier joinedIbis : joinedIbises) {
                System.err.println("Ibis joined: " + joinedIbis);
            }

            // poll for left ibises
            IbisIdentifier[] leftIbises = ibis.registry().leftIbises();
            for (IbisIdentifier leftIbis : leftIbises) {
                System.err.println("Ibis left: " + leftIbis);
            }

            // poll for died ibises
            IbisIdentifier[] diedIbises = ibis.registry().diedIbises();
            for (IbisIdentifier diedIbis : diedIbises) {
                System.err.println("Ibis died: " + diedIbis);
            }

            // sleep for a second
            Thread.sleep(1000);
        }

        // End ibis.
        ibis.end();
    }

    public static void main(String args[]) {
        try {
            new RegistryDowncalls().run();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
}
