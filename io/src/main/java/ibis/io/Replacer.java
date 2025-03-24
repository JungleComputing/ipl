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

package ibis.io;

/**
 * Object replacer, used in object serialization..
 */

public interface Replacer {
    /**
     * Replaces an object. To be used when serializing an object, to determine if
     * the object should be replaced with a stub. If so, the replace method returns
     * the stub, otherwise it returns the parameter object.
     *
     * @param v the object to be replaced
     * @return the replaced object.
     */
    public Object replace(Object v);
}
