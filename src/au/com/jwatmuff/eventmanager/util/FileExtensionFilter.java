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
