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
package ibis.ipl.impl.stacking.lrmc;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.SendPortIdentifier;

class LrmcSendPortIdentifier implements SendPortIdentifier {

    private static final long serialVersionUID = 1L;

    IbisIdentifier ibis;
    String name;

    LrmcSendPortIdentifier(IbisIdentifier ibis, String name) {
        this.ibis = ibis;
        this.name = name;
    }

    @Override
    public IbisIdentifier ibisIdentifier() {
        return ibis;
    }

    @Override
    public String name() {
        return name;
    }
}
