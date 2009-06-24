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
