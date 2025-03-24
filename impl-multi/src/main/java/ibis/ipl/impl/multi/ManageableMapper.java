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

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import ibis.ipl.Manageable;
import ibis.ipl.NoSuchPropertyException;

public class ManageableMapper implements Manageable {

    private final Map<String, Manageable> subMap;

    public ManageableMapper(Map<String, Manageable> subMap) {
        this.subMap = subMap;
    }

    @Override
    public synchronized Map<String, String> managementProperties() {
        HashMap<String, String> propertyMap = new HashMap<>();
        for (String ibisName : subMap.keySet()) {
            Manageable Manageable = subMap.get(ibisName);
            Map<String, String> props = Manageable.managementProperties();
            for (String key : props.keySet()) {
                String value = props.get(key);
                if (propertyMap.containsKey(key)) {
                    value = propertyMap.get(key) + ", " + ibisName + ": " + value;
                }
                propertyMap.put(key, value);
            }
        }
        return propertyMap;
    }

    @Override
    public synchronized String getManagementProperty(String key) throws NoSuchPropertyException {
        StringBuffer value = new StringBuffer();
        for (String ibisName : subMap.keySet()) {
            Manageable Manageable = subMap.get(ibisName);
            String ibisValue = Manageable.getManagementProperty(key);
            if (null != ibisValue) {
                if (value.length() > 0) {
                    value.append(", ");
                }
                value.append(ibisName);
                value.append(": ");
                value.append(ibisValue);
            }
        }
        return value.toString();
    }

    @Override
    public synchronized void setManagementProperties(Map<String, String> properties) throws NoSuchPropertyException {
        for (Manageable Manageable : subMap.values()) {
            Manageable.setManagementProperties(properties);
        }
    }

    @Override
    public synchronized void setManagementProperty(String key, String val) throws NoSuchPropertyException {
        for (Manageable Manageable : subMap.values()) {
            Manageable.setManagementProperty(key, val);
        }
    }

    @Override
    public synchronized void printManagementProperties(PrintStream stream) {
        for (String ibisName : subMap.keySet()) {
            Manageable Manageable = subMap.get(ibisName);
            stream.print(ibisName);
            stream.println(": ");
            Manageable.printManagementProperties(stream);
        }
    }

}
