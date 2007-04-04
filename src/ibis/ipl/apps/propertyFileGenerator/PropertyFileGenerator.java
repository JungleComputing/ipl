import ibis.ipl.impl.registry.RegistryProperties;
import ibis.ipl.IbisProperties;

import java.io.PrintStream;
import java.util.Map;
import java.util.Properties;

public class PropertyFileGenerator {

    public static void writeEntries(Map<String, String> descriptions,
            Properties properties, PrintStream out) {

        for (Map.Entry<String, String> description : descriptions.entrySet()) {
            out.println("# " + description.getValue());
            out.println("# " + description.getKey() + " = "
                    + properties.getProperty(description.getKey()));
            out.println();
        }

    }

    public static void main(String[] args) {

        try {

            PrintStream out = new PrintStream("ibis.properties.example");

            out.println("#Example ibis.properties file");
            out.println();

            out.println("#### Generic Ibis properties ####");
            out.println();
            writeEntries(IbisProperties.getDescriptions(),
            IbisProperties.getHardcodedProperties(), out);

            
            out.println("#### Ibis Registry properties ####");
            out.println();
            writeEntries(RegistryProperties.getDescriptions(),
            RegistryProperties.getHardcodedProperties(), out);


            out.flush();
            out.close();

        } catch (Exception e) {
            System.err.println("error on writing property file");
            e.printStackTrace(System.err);
        }
    }

}
