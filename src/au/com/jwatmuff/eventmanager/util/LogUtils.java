/*
 * EventManager
 * Copyright (c) 2008-2017 James Watmuff & Leonard Hall
 *
 * This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
