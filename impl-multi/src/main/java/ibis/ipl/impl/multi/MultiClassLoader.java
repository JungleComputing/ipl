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
package ibis.ipl.impl.multi;

import ibis.ipl.IbisConfigurationException;
import ibis.util.TypedProperties;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

public class MultiClassLoader extends URLClassLoader {

    public MultiClassLoader(String ibisName, TypedProperties userProperties) throws IbisConfigurationException, IOException {
        super(new URL[0], Thread.currentThread().getContextClassLoader());

        String[] jarFiles = userProperties.getStringList(MultiIbisProperties.IMPLEMENTATION_JARS + ibisName);
        if (jarFiles == null || jarFiles.length == 0) {
            throw new IbisConfigurationException("Implementation jar files not specified in property: " + MultiIbisProperties.IMPLEMENTATION_JARS + ibisName);
        }
        for (String jarFile:jarFiles) {
            File implJarFile = new File(jarFile);
            if (!implJarFile.exists()) {
                throw new IbisConfigurationException("Implementation jar file: " + jarFile + " does not exist.");
            }
            super.addURL(implJarFile.toURI().toURL());
        }
    }
}
