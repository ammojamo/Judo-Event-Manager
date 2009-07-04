/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.permissions;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class LicenseManager {
    private static final Logger log = Logger.getLogger(LicenseManager.class);

    public static final String LICENSE_EXT = ".lic";
    private List<License> licenses;

    public LicenseManager(File licenseDirectory) {
        licenses = new ArrayList<License>();

        for(File file : getLicenseFiles(licenseDirectory)) {
            try {
                licenses.add(License.loadFromFile(file));
            } catch(Exception e) {
                log.warn("Failed to load license from " + file.getName(), e);
            }
        }
    }

    public Collection<License> getLicenses() {
        return Collections.unmodifiableCollection(licenses);
    }

    private static List<File> getLicenseFiles(File licenseDirectory) {
        if(licenseDirectory.exists() && licenseDirectory.isDirectory()) {
            File[] files = licenseDirectory.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(LICENSE_EXT);
                }
            });
            return Arrays.asList(files);
        } else {
            return Collections.emptyList();
        }
    }
}
