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
package ibis.ipl.impl.multi;

import java.io.IOException;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.RegistryEventHandler;

public class MultiRegistryEventHandler implements RegistryEventHandler {

    private final RegistryEventHandler subHandler;

    private final MultiIbis ibis;

    private MultiRegistry registry;

    private String ibisName;

    public MultiRegistryEventHandler(MultiIbis ibis, RegistryEventHandler subHandler) {
        this.ibis = ibis;
        this.subHandler = subHandler;
    }

    @Override
    public synchronized void died(IbisIdentifier corpse) {
        while (registry == null) {
            try {
                wait();
            } catch (InterruptedException e) {
                // Ignored
            }
        }
        try {
            MultiIbisIdentifier id = ibis.mapIdentifier(corpse, ibisName);
            if (!registry.died.containsKey(id)) {
                registry.died.put(id, id);
                subHandler.died(id);
            }
        } catch (IOException e) {
            // TODO What the hell to do.
        }
    }

    @Override
    public synchronized void electionResult(String electionName, IbisIdentifier winner) {
        while (registry == null) {
            try {
                wait();
            } catch (InterruptedException e) {
                // Ignored
            }
        }
        try {
            MultiIbisIdentifier id = ibis.mapIdentifier(winner, ibisName);
            if (!registry.elected.containsKey(electionName)) {
                registry.elected.put(electionName, id);
                subHandler.electionResult(electionName, id);
            } else {
                MultiIbisIdentifier oldWinner = registry.elected.get(electionName);
                if (!oldWinner.equals(id)) {
                    registry.elected.put(electionName, id);
                    subHandler.electionResult(electionName, id);
                }
            }
        } catch (IOException e) {
            // TODO What the hell to do
        }
    }

    @Override
    public void gotSignal(String signal, IbisIdentifier source) {
        subHandler.gotSignal(signal, source);
    }

    @Override
    public synchronized void joined(IbisIdentifier joinedIbis) {
        while (registry == null) {
            try {
                wait();
            } catch (InterruptedException e) {
                // Ignored
            }
        }
        try {
            MultiIbisIdentifier id = ibis.mapIdentifier(joinedIbis, ibisName);
            if (!registry.joined.containsKey(id)) {
                registry.joined.put(id, id);
                subHandler.joined(id);
            }
        } catch (IOException e) {
            // TODO What the hell to do here?
        }
    }

    @Override
    public synchronized void left(IbisIdentifier leftIbis) {
        while (registry == null) {
            try {
                wait();
            } catch (InterruptedException e) {
                // Ignored
            }
        }
        try {
            MultiIbisIdentifier id = ibis.mapIdentifier(leftIbis, ibisName);
            if (!registry.left.containsKey(id)) {
                registry.left.put(id, id);
                subHandler.left(id);
            }
        } catch (IOException e) {
            // TODO What the hell to do here?
        }
    }

    public synchronized void setName(String ibisName) {
        this.ibisName = ibisName;
    }

    public synchronized void setRegistry(MultiRegistry registry) {
        this.registry = (MultiRegistry) ibis.registry();
        notifyAll();
    }

    @Override
    public void poolClosed() {
        // FIXME: implement
    }

    @Override
    public void poolTerminated(IbisIdentifier source) {
        // FIXME: implement
    }

}
