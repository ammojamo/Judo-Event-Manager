package au.com.jwatmuff.eventmanager.util;

import java.io.File;
import java.io.FilenameFilter;

/**
 *
 * @author James
 */
public class FileExtensionFilter implements FilenameFilter {
    public String[] extensions;

    public FileExtensionFilter(final String... extensions) {
        this.extensions = new String[extensions.length];
        for(int i = 0; i < extensions.length; i++)
            this.extensions[i] = (extensions[i].startsWith(".") ? "" : ".") + extensions[i].toLowerCase();
    }

    public boolean accept(File dir, String name) {
        for(String extension : extensions)
            if(name.toLowerCase().endsWith(extension)) return true;
        return false;
    }
}
