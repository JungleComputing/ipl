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
package ibis.ipl.server;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.support.management.AttributeDescription;

/**
 * Interface to the management service. Mostly for getting management info from the
 * server.
 * 
 * @ibis.experimental
 */
public interface ManagementServiceInterface {

    /**
     * Obtains the attributes from the specified Ibis instance, one object per attribute
     * description.
     * @param ibis the Ibis instance to obtain attributes from.
     * @param descriptions the attribute descriptions.
     * @return the attributes.
     * @throws Exception is thrown in case of trouble.
     */
    public abstract Object[] getAttributes(IbisIdentifier ibis,
            AttributeDescription... descriptions) throws Exception;

}