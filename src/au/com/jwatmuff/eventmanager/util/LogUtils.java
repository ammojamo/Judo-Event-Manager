package au.com.jwatmuff.eventmanager.util;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

/**
 *
 * @author James
 */
public class LogUtils {
    private static final Logger log = Logger.getLogger(LogUtils.class);

    /* reconfigures all FileAppenders in log4j to output to the specified directory */
    public static void reconfigureFileAppenders(String log4jProperties, File directory) {
        try {
            // Add logging output to new working directory
            Properties logProperties = PropertiesLoaderUtils.loadProperties(new ClassPathResource(log4jProperties));
            for(String key : logProperties.stringPropertyNames()) {
                if(key.matches(".*\\.appender\\..*\\.File$")) {
                    String value = logProperties.getProperty(key);
                    File file = new File(value);
                    file = new File(directory, file.getName());
                    logProperties.setProperty(key, file.getAbsolutePath());
                }
            }
            PropertyConfigurator.configure(logProperties);

        } catch(IOException e) {
            log.warn("IOException while reconfiguring logging to writable directory", e);
        }
    }
}
