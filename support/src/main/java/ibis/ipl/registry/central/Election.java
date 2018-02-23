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
package ibis.ipl.registry.central;

import ibis.ipl.impl.IbisIdentifier;

public class Election {
    
    private final Event event;

    public Election(Event event) {
        this.event = event;
    }

    public String getName() {
        return event.getDescription();
    }

    public IbisIdentifier getWinner() {
        return event.getIbis();
    }
    
    public Event getEvent() {
        return event;
    }
    
    public String toString() {
        return getName() + " won by " + getWinner();
    }
}
