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

import java.util.HashMap;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReceivePortIdentifier;

public class MultiReceivePortIdentifier implements ReceivePortIdentifier {

    /**
     * Serial Version ID - Generated
     */
    private static final long serialVersionUID = 3918962573170503300L;
    private final String name;
    private final IbisIdentifier id;

    private final HashMap<String, ReceivePortIdentifier> subIds = new HashMap<>();

    public MultiReceivePortIdentifier(IbisIdentifier id, String name) {
        this.name = name;
        this.id = id;
    }

    @Override
    public IbisIdentifier ibisIdentifier() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    ReceivePortIdentifier getSubId(String ibisName) {
        return subIds.get(ibisName);
    }

    void addSubId(String ibisName, ReceivePortIdentifier subId) {
        subIds.put(ibisName, subId);
    }
}
