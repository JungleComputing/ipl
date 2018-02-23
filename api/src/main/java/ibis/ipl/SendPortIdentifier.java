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
/* $Id$ */

package ibis.ipl;

/**
 * Identifies a {@link ibis.ipl.SendPort SendPort}.
 */
public interface SendPortIdentifier extends java.io.Serializable {
    /**
     * Returns the name of the {@link ibis.ipl.SendPort SendPort}
     * corresponding to this identifier.
     * @return
     *          the name of the sendport.
     */
    public String name();

    /**
     * Returns the {@link ibis.ipl.IbisIdentifier IbisIdentifier} of the
     * {@link ibis.ipl.SendPort SendPort} corresponding to this identifier.
     * @return
     *          the ibis identifier.
     */
    public IbisIdentifier ibisIdentifier();

    /**
     * The hashCode method is mentioned here just as a reminder that an
     * implementation must probably redefine it, because two objects
     * representing the same <code>SendPortIdentifier</code> must result
     * in the same hashcode (and compare equal).
     * To explicitly specify it in the interface does not help, because
     * java.lang.Object already implements it, but, anyway, here it is.
     * 
     * {@inheritDoc}
     */
    public int hashCode();

    /**
     * The equals method is mentioned here just as a reminder that an
     * implementation must probably redefine it, because two objects
     * representing the same <code>SendPortIdentifier</code> must
     * compare equal (and result in the same hashcode).
     * To explicitly specify it in the interface does not help, because
     * java.lang.Object already implements it, but, anyway, here it is.
     * 
     * {@inheritDoc}
     */
    public boolean equals(Object other);
}
