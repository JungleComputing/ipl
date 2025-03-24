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

package ibis.ipl.impl.nio;

/**
 * General configuration file for NioIbis.
 */
interface Config {

    /** Byte buffer size used. Must be a multiple of eight. */
    static final int BYTE_BUFFER_SIZE = 60 * 1024;

    /**
     * Buffer sized used for primitive buffers. Must be a multiple of eight.
     */
    static final int PRIMITIVE_BUFFER_SIZE = 6 * 1024;
}
