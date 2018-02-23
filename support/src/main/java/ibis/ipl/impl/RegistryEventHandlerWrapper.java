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
package ibis.ipl.impl;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.RegistryEventHandler;

public class RegistryEventHandlerWrapper implements RegistryEventHandler {
    
    private final RegistryEventHandler handler;
    private final Ibis ibis;
    
    public RegistryEventHandlerWrapper(RegistryEventHandler h, Ibis i) {
	handler = h;
	ibis = i;
    }

    public void died(IbisIdentifier corpse) {
	if (handler != null) {
	    handler.died(corpse);
	}
	ibis.died(corpse);
    }

    public void electionResult(String electionName, IbisIdentifier winner) {
	if (handler != null) {
	    handler.electionResult(electionName, winner);
	}
    }

    public void gotSignal(String signal, IbisIdentifier source) {
	if (handler != null) {
	    handler.gotSignal(signal, source);
	}
    }

    public void joined(IbisIdentifier joinedIbis) {
	if (handler != null) {
	    handler.joined(joinedIbis);
	}
    }

    public void left(IbisIdentifier leftIbis) {
	if (handler != null) {
	    handler.left(leftIbis);
	}
	ibis.left(leftIbis);
    }

    public void poolClosed() {
	if (handler != null) {
	    handler.poolClosed();
	}
    }

    public void poolTerminated(IbisIdentifier source) {
	if (handler != null) {
	    handler.poolTerminated(source);
	}
    }
}
