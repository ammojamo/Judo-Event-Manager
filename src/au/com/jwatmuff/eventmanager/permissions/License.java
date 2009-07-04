/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.permissions;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 *
 * @author James
 */
public class License {
    public static final String NAME_PROPERTY = "name";
    public static final String EXPIRY_PROPERTY = "expiry";
    public static final String TYPE_PROPERTY = "type";
    public static final String PHONE_PROPERTY = "contact";
    public static final String HASH_PROPERTY = "hash";
    
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

    private String name;
    private String contactPhone;
    private Date expiry;
    private LicenseType type;

    public License(String name, String contactPhone, Date expiry, LicenseType type) {
        this.name = name;
        this.contactPhone = contactPhone;
        this.expiry = expiry;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getContactPhoneNumber() {
        return contactPhone;
    }

    public Date getExpiry() {
        return new Date(expiry.getTime());
    }

    public LicenseType getType() {
        return type;
    }

    public int getHash() {
        return new int[] {
            name.trim().toUpperCase().hashCode(),
            expiry.hashCode(),
            contactPhone.replaceAll("[^0-9]", "").hashCode(),
            type.hashCode()
        }.hashCode();
    }

    public String getKey() {
        return dateFormat.format(expiry) + "-" +
                type.toString() + "-" +
                Integer.toString(hashCode(), 16);
    }

    public static License loadFromFile(File file) {
        try {
            Properties props = new Properties();
            props.load(new FileReader(file));

            String name = props.getProperty(NAME_PROPERTY);
            Date expiry = dateFormat.parse(props.getProperty(EXPIRY_PROPERTY));
            String contactPhone = props.getProperty(PHONE_PROPERTY);
            LicenseType type = LicenseType.valueOf(props.getProperty(TYPE_PROPERTY));
            int hash = Integer.valueOf(props.getProperty(HASH_PROPERTY));
            if(name == null || expiry == null || type == null || hash == 0)
                throw new RuntimeException("Missing or invalid properties");

            License license =  new License(name, contactPhone, expiry, type);

            if(license.getHash() != hash)
                throw new RuntimeException("License key invalid");

            return license;

        } catch(Exception e) {
            throw new RuntimeException("Unable to load license from file");
        }
    }

    public static void saveToFile(License license, File file) throws IOException {
        Properties props = new Properties();

        props.put(NAME_PROPERTY, license.getName());
        props.put(EXPIRY_PROPERTY, dateFormat.format(license.getExpiry()));
        props.put(TYPE_PROPERTY, license.getType().toString());
        props.put(HASH_PROPERTY, Integer.toString(license.getHash(), 16));

        props.store(new FileWriter(file),
                "EventManager License File\n" +
                "WARNING! Editing this file will cause the license to stop working");
    }

    public static License fromKey(String name, String contactPhone, String key) {
        try {
            String[] keyParts = key.split("-");
            Date expiry = dateFormat.parse(keyParts[0]);
            LicenseType type = LicenseType.valueOf(keyParts[1]);
            int hash = Integer.valueOf(keyParts[2], 16);

            License license = new License(name, contactPhone, expiry, type);

            if(license.hashCode() == hash)
                return license;
            else
                return null;
        } catch(Exception e) {
            return null;
        }
    }

    public static boolean isKeyValid(String name, String contactPhone, String key) {
        return (fromKey(name, contactPhone, key) != null);
    }
}
