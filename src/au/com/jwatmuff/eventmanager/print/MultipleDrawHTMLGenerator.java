package au.com.jwatmuff.eventmanager.print;

import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.genericdb.Database;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import org.apache.velocity.context.Context;

/**
 *
 * @author James
 */
public class MultipleDrawHTMLGenerator extends HTMLGenerator {
    private List<Pool> pools;
    private Database database;
    private boolean showResults;

    private VelocityHTMLGenerator footerGenerator = new VelocityHTMLGenerator() {
        @Override
        public String getTemplateName() { return "draw-footer.html"; }

        @Override
        public void populateContext(Context context) { }
    };

    private VelocityHTMLGenerator headerGenerator = new VelocityHTMLGenerator() {
        @Override
        public String getTemplateName() { return "draw-header.html"; }

        @Override
        public void populateContext(Context context) { }
    };

    public MultipleDrawHTMLGenerator(Database database, List<Pool> pools, boolean showResults) {
        this.pools = new ArrayList<Pool>(pools);
        this.database = database;
        this.showResults = showResults;
    }

    @Override
    public void generate(Writer writer) {
        headerGenerator.generate(writer);

        boolean first = true;
        for(Pool pool : pools) {
            new DrawHTMLGenerator(database, pool.getID(), showResults, false, first).generate(writer);
            first = false;
        }

        footerGenerator.generate(writer);
    }
}
