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
package ibis.io;

import java.io.IOException;

/**
 * Abstract class that implements object reads for any kind of object.
 * The idea is that there is a separate reader for each kind of object,
 * so that runtime tests can be avoided.
 */
abstract class IbisReader {
    abstract Object readObject(IbisSerializationInputStream in,
            AlternativeTypeInfo t, int typeHandle)
            throws IOException, ClassNotFoundException;
}
