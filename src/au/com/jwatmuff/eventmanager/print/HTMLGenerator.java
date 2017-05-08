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

package au.com.jwatmuff.eventmanager.print;

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public abstract class HTMLGenerator {
    public abstract void generate(Writer writer);
    
    private static final Logger log = Logger.getLogger(HTMLGenerator.class);

    public void openInBrowser() {
        FileWriter fw = null;
        try {
            File f = File.createTempFile("eventmanager", ".html");
            fw = new FileWriter(f);
            generate(fw);
            fw.close();
            Desktop.getDesktop().browse(f.toURI());
        } catch (IOException e) {
            log.error("Error displaying/generating HTML", e);
        } finally {
            try {
                fw.close();
            } catch (IOException e) {
                log.error("Error closing HTML file writer", e);
            }
        }
    }
}
