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
 * Signals that there was an error in the Ibis configuration.
 * An <code>IbisConfigurationException</code> is thrown to indicate
 * that there is something wrong in the way Ibis was configured,
 * for instance because a method was invoked that requires capabilities
 * that were not configured.
 */
public class IbisConfigurationException extends IbisRuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a <code>IbisConfigurationException</code> with
     * <code>null</code> as its error detail message.
     */
    public IbisConfigurationException() {
        super();
    }

    /**
     * Constructs a <code>IbisConfigurationException</code> with
     * the specified detail message.
     *
     * @param detailMessage
     *          the detail message
     */
    public IbisConfigurationException(String detailMessage) {
        super(detailMessage);
    }

    /**
     * Constructs a <code>IbisConfigurationException</code> with
     * the specified detail message and cause.
     *
     * @param detailMessage
     *          the detail message
     * @param cause
     *          the cause
     */
    public IbisConfigurationException(String detailMessage, Throwable cause) {
        super(detailMessage, cause);
    }

    /**
     * Constructs a <code>IbisConfigurationException</code> with
     * the specified cause.
     *
     * @param cause
     *          the cause
     */
    public IbisConfigurationException(Throwable cause) {
        super(cause);
    }
}
