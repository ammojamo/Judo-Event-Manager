package au.com.jwatmuff.eventmanager.util;

import java.io.File;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
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

    public static void setupUncaughtExceptionHandler() {
        UncaughtExceptionHandler handler = new SimpleUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(handler);
        // Handle Swing EDT exceptions
        System.setProperty("sun.awt.exception.handler", SimpleUncaughtExceptionHandler.class.getName());
    }

    public static class SimpleUncaughtExceptionHandler implements UncaughtExceptionHandler {
        public void uncaughtException(Thread t, Throwable e) {
            log.error("Uncaught exception on thread " + t, e);
        }
        public void handle(Throwable e) {
            log.error("Uncaught exception on EDT", e);
        }
    }
}
