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
package ibis.ipl.registry.gossip;

final class Protocol {
    
    public static final int VIRTUAL_PORT = 303;

    // opcodes

    public static final byte OPCODE_ARRG_GOSSIP = 0;

    public static final byte OPCODE_SIGNAL = 1;

    public static final byte OPCODE_LEAVE = 2;

    public static final byte OPCODE_GOSSIP = 3;

    public static final byte OPCODE_PING = 4;

    public static final int NR_OF_OPCODES = 5;

    public static final String[] OPCODE_NAMES =
        { "ARRG_GOSSIP", "SIGNAL", "LEAVE", "GOSSIP", "PING" };

    // replies

    static final byte REPLY_OK = 1;

    static final byte REPLY_ERROR = 2;

    // misc "options"

    public static final byte MAGIC_BYTE = 54;

    public static String opcodeString(byte opcode) {
        if (opcode == OPCODE_ARRG_GOSSIP) {
            return "ARRG_GOSSIP";
        } else {
            return "UNKNOWN";
        }
    }

}
