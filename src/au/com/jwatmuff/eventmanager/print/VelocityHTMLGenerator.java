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
        }
    }
    
    public abstract String getTemplateName();
    public abstract void populateContext(Context context);
}
