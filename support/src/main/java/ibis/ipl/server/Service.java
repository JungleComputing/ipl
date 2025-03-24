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

/**
 * Interface for an Ibis Service. Any service that wants to be automatically
 * started by the ibis-server must implement this interface. It must also have a
 * constructor with the signature: Service(TypedProperties properties,
 * VirtualSocketFactory factory).
 */
public interface Service {

    /**
     * Returns the name of this service.
     *
     * @return name of this service
     */
    String getServiceName();

    /**
     * Called when the server stops.
     *
     * @param deadline a service is allowed to block in this function for a while if
     *                 it is busy. However, it may not block beyond the deadline.
     */
    void end(long deadline);
}
