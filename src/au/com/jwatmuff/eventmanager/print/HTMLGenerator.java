/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
