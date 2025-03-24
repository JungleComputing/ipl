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
package ibis.ipl.impl;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import ibis.ipl.NoSuchPropertyException;

public abstract class Manageable implements ibis.ipl.Manageable {

    private HashSet<String> validKeys = new HashSet<>();

    /** Map for implementing the dynamic properties. */
    private HashMap<String, String> properties = new HashMap<>();

    @Override
    public synchronized Map<String, String> managementProperties() {
        updateProperties();
        return new HashMap<>(properties);
    }

    @Override
    public synchronized void setManagementProperties(Map<String, String> properties) throws NoSuchPropertyException {
        HashSet<String> keys = new HashSet<>(properties.keySet());

        for (String key : keys) {
            if (!validKeys.contains(key)) {
                throw new NoSuchPropertyException("Invalid key: " + key);
            }
        }
        this.properties.putAll(properties);
        doProperties(properties);
    }

    protected void doProperties(Map<String, String> properties) {
        // default implementation is empty.
        // This method is called when the user calls setManagementProperties,
        // so that implementations can adapt their internal matching variables.
    }

    @Override
    public synchronized String getManagementProperty(String key) throws NoSuchPropertyException {
        if (!validKeys.contains(key)) {
            throw new NoSuchPropertyException("Invalid key: " + key);
        }
        updateProperties();
        return properties.get(key);
    }

    @Override
    public synchronized void setManagementProperty(String key, String val) throws NoSuchPropertyException {
        if (!validKeys.contains(key)) {
            throw new NoSuchPropertyException("Invalid key: " + key);
        }
        properties.put(key, val);
        doProperty(key, val);
    }

    protected void doProperty(String key, String value) {
        // default implementation is empty.
        // This method is called when the user calls setManagementProperty,
        // so that implementations can adapt their internal matching variables.
    }

    protected void addValidKey(String key) {
        validKeys.add(key);
    }

    protected synchronized void setProperty(String key, String val) {
        properties.put(key, val);
    }

    protected abstract void updateProperties();

    @Override
    public void printManagementProperties(PrintStream stream) {
        updateProperties();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            stream.println(entry.getKey() + " " + entry.getValue());
        }
    }
}
