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
package ibis.ipl.registry.central;

public final class Protocol {

    public static final byte MAGIC_BYTE = 54;

    public static final int VIRTUAL_PORT = 302;

    // opcodes

    public static final byte OPCODE_JOIN = 0;

    public static final byte OPCODE_LEAVE = 1;

    public static final byte OPCODE_GOSSIP = 2;

    public static final byte OPCODE_ELECT = 3;

    public static final byte OPCODE_SEQUENCE_NR = 4;

    public static final byte OPCODE_DEAD = 5;

    public static final byte OPCODE_MAYBE_DEAD = 6;

    public static final byte OPCODE_SIGNAL = 7;

    public static final byte OPCODE_PING = 8;

    public static final byte OPCODE_PUSH = 9;

    public static final byte OPCODE_BROADCAST = 10;

    public static final byte OPCODE_FORWARD = 11;

    public static final byte OPCODE_GET_STATE = 12;

    public static final byte OPCODE_HEARTBEAT = 13;

    public static final byte OPCODE_TERMINATE = 14;

    public static final byte OPCODE_ADD_TOKENS = 15;

    public static final byte OPCODE_GET_TOKEN = 16;

    public static final int NR_OF_OPCODES = 17;

    public static final String[] OPCODE_NAMES = { "JOIN", "LEAVE", "GOSSIP", "ELECT", "SEQUENCE_NR", "DEAD", "MAYBE_DEAD", "SIGNAL", "PING", "PUSH",
            "BROADCAST", "FORWARD", "GET_STATE", "HEARTBEAT", "TERMINATE", "ADD_TOKENS", "GET_TOKEN" };
}
