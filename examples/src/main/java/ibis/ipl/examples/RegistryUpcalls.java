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
import ibis.ipl.RegistryEventHandler;

/**
 * This program shows how to handle events from the registry using upcalls. It
 * will run for 30 seconds, then stop. You can start as many instances of this
 * application as you like.
 */

public class RegistryUpcalls implements RegistryEventHandler {

    IbisCapabilities ibisCapabilities = new IbisCapabilities(IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED);

    // Methods of the registry event handler. We only implement the
    // join/leave/died methods, as signals and elections are disabled

    @Override
    public void joined(IbisIdentifier joinedIbis) {
        System.err.println("Got event from registry: " + joinedIbis + " joined pool");
    }

    @Override
    public void died(IbisIdentifier corpse) {
        System.err.println("Got event from registry: " + corpse + " died!");
    }

    @Override
    public void left(IbisIdentifier leftIbis) {
        System.err.println("Got event from registry: " + leftIbis + " left");
    }

    @Override
    public void electionResult(String electionName, IbisIdentifier winner) {
        System.err.println("Got event from registry: " + winner + " won election " + electionName);
    }

    @Override
    public void gotSignal(String signal, IbisIdentifier source) {
        System.err.println("Got event from registry: signal \"" + signal + "\" from " + source);
    }

    @Override
    public void poolClosed() {
        System.err.println("Got event from registry: pool closed");
    }

    @Override
    public void poolTerminated(IbisIdentifier source) {
        System.err.println("Got event from registry: pool terminated by " + source);
    }

    private void run() throws Exception {
        // Create an ibis instance, pass ourselves as the event handler
        Ibis ibis = IbisFactory.createIbis(ibisCapabilities, this);
        ibis.registry().enableEvents();

        // sleep for 30 seconds
        Thread.sleep(30000);

        // End ibis.
        ibis.end();
    }

    public static void main(String args[]) {
        try {
            new RegistryUpcalls().run();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

}
