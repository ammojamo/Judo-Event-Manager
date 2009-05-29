/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.print;

import java.io.Writer;
import org.apache.log4j.Logger;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.ToolManager;

/**
 *
 * @author James
 */
public abstract class VelocityHTMLGenerator extends HTMLGenerator {
    private static final Logger log = Logger.getLogger(VelocityHTMLGenerator.class);

    public static final String TEMPLATE_PREFIX = "resources/html/";

    @Override
    public void generate(Writer writer) {
        try {
            // init velocity
            Velocity.init();

            // setup context (this is probably v. inefficient)
            ToolManager manager = new ToolManager();
            Context c = manager.createContext();

            // populate context
            populateContext(c);

            // merge with template
            Velocity.mergeTemplate(
                    TEMPLATE_PREFIX + getTemplateName(),
                    Velocity.ENCODING_DEFAULT,
                    c,
                    writer);
        } catch(Exception e) {
            log.error("Error generating HTML", e);
            return;
        }
    }
    
    public abstract String getTemplateName();
    public abstract void populateContext(Context context);
}
