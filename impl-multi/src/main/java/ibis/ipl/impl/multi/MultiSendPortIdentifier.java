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

import ibis.ipl.IbisIdentifier;
import ibis.ipl.SendPortIdentifier;

public class MultiSendPortIdentifier implements SendPortIdentifier {

    /**
     * Serial Version UID - Generated
     */
    private static final long serialVersionUID = -1400675486608710003L;

    private final IbisIdentifier id;
    private final String name;

    public MultiSendPortIdentifier(IbisIdentifier identifier, String name) {
        this.id = identifier;
        this.name = name;
    }

    @Override
    public IbisIdentifier ibisIdentifier() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

}
