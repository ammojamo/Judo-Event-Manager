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

package au.com.jwatmuff.eventmanager.model.config;

import au.com.jwatmuff.eventmanager.util.FileExtensionFilter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class ConfigurationFile {
    private static final Logger log = Logger.getLogger(ConfigurationFile.class);

    private static final String DRAW_CONFIG_DIR = "resources";

    private Properties properties;

    private ConfigurationFile(Properties properties) {
        this.properties = properties;
    }

    public String getName() {
        return properties.getProperty("name");
    }

    public String getDrawName(int players) {
//        Check for value in this paramater file
        for(int numberOfPlayers = players; numberOfPlayers <=64; numberOfPlayers++)
            if(properties.containsKey("" + numberOfPlayers))
                return properties.getProperty("" + numberOfPlayers);

//        Check for value in this default paramater file
        for(int numberOfPlayers = players; numberOfPlayers <=64; numberOfPlayers++)
            if(properties.getProperty("" + numberOfPlayers) != null)
                return properties.getProperty("" + numberOfPlayers);
        
//        return null - as it was before I changed anything :)
        return properties.getProperty("" + players);
    }

    public String getProperty(String propertyName) {
        return properties.getProperty(propertyName);
    }

    public int getIntegerProperty(String propertyName, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(propertyName));
        } catch(Exception e) {
            return defaultValue;
        }
    }

    public double getDoubleProperty(String propertyName, double defaultValue) {
        try {
            return Double.parseDouble(properties.getProperty(propertyName));
        } catch(Exception e) {
            return defaultValue;
        }
    }

    public boolean getBooleanProperty(String propertyName, boolean defaultValue) {
        try {
            return properties.getProperty(propertyName).equalsIgnoreCase("true");
        } catch(Exception e) {
            return defaultValue;
        }
    }

    /*** Static utility methods ***/

    private static Map<String, ConfigurationFile> configurations = new HashMap<String, ConfigurationFile>();

    static {
        updateConfigurations();
    }

    private static void updateConfigurations() {
        File configDir = new File(DRAW_CONFIG_DIR);
        for(File f : configDir.listFiles(new FileExtensionFilter("properties"))) {
            Properties props = new Properties();
            try {
                props.load(new FileReader(f));
                if(props.containsKey("name")) {
                    String name = props.getProperty("name");
                    configurations.put(name, new ConfigurationFile(props));
                }
            } catch(IOException e) {
                log.error("IOException loading draw configurations", e);
            }
        }
    }

    public static List<ConfigurationFile> getConfigurations() {
        return new ArrayList<ConfigurationFile>(configurations.values());
    }

    public static String[] getConfigurationNames() {
        ArrayList<String> names = new ArrayList<String>();
        for(ConfigurationFile config : configurations.values())
            names.add(config.getName());

        // Sort alphabetically
        Collections.sort(names);
        // If there is a draw named Default, put it first
        if(names.remove("Default")) names.add(0, "Default");
        // convert to array
        return names.toArray(new String[] {});
    }

    public static ConfigurationFile getConfiguration(String name) {
        if(!configurations.containsKey(name))
            name = "Default";
        return configurations.get(name);
    }
}
