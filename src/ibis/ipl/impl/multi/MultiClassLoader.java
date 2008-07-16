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
