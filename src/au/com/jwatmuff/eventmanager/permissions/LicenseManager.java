/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.permissions;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class LicenseManager {
    private static final Logger log = Logger.getLogger(LicenseManager.class);

    public static final String LICENSE_FILE = "license.lic";
    private License license;
    private File directory;

    public LicenseManager(File licenseDirectory) {
        this.directory = licenseDirectory;
        try {
            loadLicense();
            updatePermissionChecker();
        } catch(Exception e) {
        }
    }

    public License getLicense() {
        return license;
    }

    public void setLicense(License license) throws IOException {
        this.license = license;
        updatePermissionChecker();
        saveLicense();
    }

    public final void updatePermissionChecker() {
        LicenseType licenseType;
        if(license == null || license.getExpiry().before(new Date())) {
            licenseType = LicenseType.DEFAULT_LICENSE;
        } else {
            licenseType = license.getType();
        }
        PermissionChecker.setLicenseType(licenseType);
    }

    private void loadLicense() {
        File file = new File(directory, LICENSE_FILE);
        if(file.exists()) {
            try {
                license = License.loadFromFile(file);
            } catch(Exception e) {
                log.error("FAILED TO LOAD LICENSE", e);
                license = null;
            }
        } else {
            license = null;
        }
    }

    private void saveLicense() throws IOException {
        File file = new File(directory, LICENSE_FILE);
        License.saveToFile(license, file);
    }

    /******** For Multiple Licenses: ********/
    public static final String LICENSE_EXT = ".lic";
    private List<License> licenses;

    public Collection<License> getLicenses() {
        return Collections.unmodifiableCollection(licenses);
    }

    private void loadLicensesInDirectory(File licenseDirectory) {
        licenses = new ArrayList<License>();

        for(File file : getLicenseFiles(licenseDirectory)) {
            try {
                licenses.add(License.loadFromFile(file));
            } catch(Exception e) {
                log.warn("Failed to load license from " + file.getName(), e);
            }
        }
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
