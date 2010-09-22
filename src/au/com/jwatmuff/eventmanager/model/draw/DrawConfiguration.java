package au.com.jwatmuff.eventmanager.model.draw;

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
public class DrawConfiguration {
    private static final Logger log = Logger.getLogger(DrawConfiguration.class);

    private static final String DRAW_CONFIG_DIR = "resources/draw";

    private Properties properties;

    private DrawConfiguration(Properties properties) {
        this.properties = properties;
    }

    public String getName() {
        return properties.getProperty("name");
    }

    public String getDrawName(int players) {
        return properties.getProperty("" + players);
    }

    /*** Static utility methods ***/

    private static Map<String, DrawConfiguration> drawConfigurations = new HashMap<String, DrawConfiguration>();

    static {
        updateDrawConfigurations();
    }

    private static void updateDrawConfigurations() {
        File configDir = new File(DRAW_CONFIG_DIR);
        for(File f : configDir.listFiles(new FileExtensionFilter("properties"))) {
            Properties props = new Properties();
            try {
                props.load(new FileReader(f));
                if(props.containsKey("name")) {
                    String name = props.getProperty("name");
                    drawConfigurations.put(name, new DrawConfiguration(props));
                }
            } catch(IOException e) {
                log.error("IOException loading draw configurations", e);
            }
        }
    }

    public static List<DrawConfiguration> getDrawConfigurations() {
        return new ArrayList<DrawConfiguration>(drawConfigurations.values());
    }

    public static String[] getDrawConfigurationNames() {
        ArrayList<String> names = new ArrayList<String>();
        for(DrawConfiguration config : drawConfigurations.values())
            names.add(config.getName());

        // Sort alphabetically
        Collections.sort(names);
        // If there is a draw named Default, put it first
        if(names.remove("Default")) names.add(0, "Default");
        // convert to array
        return names.toArray(new String[] {});
    }

    public static DrawConfiguration getDrawConfiguration(String name) {
        if(!drawConfigurations.containsKey(name))
            name = "Default";
        return drawConfigurations.get(name);
    }
}
