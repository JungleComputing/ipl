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

class ServerConnectionProtocol {

    public static final byte MAGIC_BYTE = 74;

    public static final int VIRTUAL_PORT = 300;

    // opcodes

    public static final byte OPCODE_GET_ADDRESS = 0;

    public static final byte OPCODE_GET_SERVICE_NAMES = 1;

    public static final byte OPCODE_GET_HUBS = 2;

    public static final byte OPCODE_ADD_HUBS = 3;

    public static final byte OPCODE_END = 4;

    public static final byte OPCODE_REGISTRY_GET_POOLS = 5;
    
    public static final byte OPCODE_REGISTRY_GET_LOCATIONS = 6;
    
    public static final byte OPCODE_REGISTRY_GET_POOL_SIZES = 7;

    public static final byte OPCODE_REGISTRY_GET_MEMBERS = 8;

    public static final byte OPCODE_MANAGEMENT_GET_ATTRIBUTES = 9;
}
