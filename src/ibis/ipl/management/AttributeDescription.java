package ibis.ipl.management;

/**
 * Description of a management attribute. Basically a set of an MBean name, and
 * a attribute within that bean.
 * 
 * @author ndrost
 *
 * @ibis.experimental
 */
public class AttributeDescription {
    
    private final String beanName;
    
    private final String attribute;
    
    public AttributeDescription(String beanName, String attribute) {
        this.beanName = beanName;
        this.attribute = attribute;
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

}
