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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

/**
 * Description of a management attribute. Basically a set of an MBean name, and
 * a attribute within that bean.
 *
 * @author ndrost
 *
 * @ibis.experimental
 */
public class AttributeDescription implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String beanName;

    private final String attribute;

    public AttributeDescription(String beanName, String attribute) {
        this.beanName = beanName;
        this.attribute = attribute;
    }

    public AttributeDescription(DataInput input) throws IOException {
        beanName = input.readUTF();
        attribute = input.readUTF();
    }

    /**
     * @return the beanName
     */
    public String getBeanName() {
        return beanName;
    }

    /**
     * @return the attribute
     */
    public String getAttribute() {
        return attribute;
    }

    public void writeTo(DataOutput out) throws IOException {
        out.writeUTF(beanName);
        out.writeUTF(attribute);
    }

}
