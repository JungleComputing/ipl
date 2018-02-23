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
package ibis.util;

import java.util.Properties;

/**
 * A properties object for this package.
 */
class UtilProperties extends TypedProperties {

    private static final long serialVersionUID = 1L;

    private static String[] prefs = { "ibis.util.ip.", "ibis.util.monitor." };
 
    /** Constructs an empty typed properties object. */
    public UtilProperties() {
        super();
    }

    /**
     * Constructs a typed properties object with the specified defaults.
     * @param defaults the defaults.
     */
    public UtilProperties(Properties defaults, String prefix, String[] props) {
        super(defaults);
        checkProperties("ibis.util.", null, prefs, true);
        checkProperties(prefix, props, null, true);
    }
}
