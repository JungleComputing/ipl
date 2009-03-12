package ibis.ipl.impl.util;

import ibis.ipl.IbisProperties;
import ibis.ipl.registry.central.RegistryProperties;

import java.io.PrintStream;
import java.util.Map;
import java.util.Properties;

public class PropertyFileGenerator {

    public static void writeEntries(Map<String, String> descriptions,
            Properties properties, PrintStream out) {

        for (Map.Entry<String, String> description : descriptions.entrySet()) {
            out.println("## " + description.getValue());
            String value = properties.getProperty(description.getKey());
            if (value == null) {
                out.println("# " + description.getKey() + " = ");
            } else {
                out.println("# " + description.getKey() + " = " + value);
            }
            out.println();
        }

    }

    public static void main(String[] args) {

        try {
            System.out.println("writing ibis.properties.example file");

            PrintStream out = new PrintStream("ibis.properties.example");

            out.println("# Example ibis.properties file\n"
                    + "\n"
                    + "# An ibis property file can be used to change settings of ibis. Ibis will look\n"
                    + "# for a file named \"ibis.properties\" on the classpath, the current directory,\n"
                    + "# and in the location set in the \"ibis.properties.file\" system property.\n"
                    + "# Alternatively, a user can specify these properties directly as system\n"
                    + "# properties using the -D option of java.");

            out.println();
            out.println("#This file lists properties valid for various parts of ibis");
            out.println("#Also listed are a description, and the default value of this property, if any");
            out.println();

            out.println("#### Generic Ibis properties ####");
            out.println();
            writeEntries(IbisProperties.getDescriptions(),
                IbisProperties.getHardcodedProperties(), out);
            out.println();
            out.println();

            out.println("#### Ibis Central Registry properties ####");
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
