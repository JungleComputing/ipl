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
package ibis.ipl.support.management;

public class Protocol {

    public static final int VIRTUAL_PORT = 304;

    public static final byte MAGIC_BYTE = 55;

    public static final byte OPCODE_GET_MONITOR_INFO = 0;

    public static final byte NR_OF_OPCODES = 1;

    public static final String[] OPCODE_NAMES = { "GET_MONITOR_INFO" };

}
