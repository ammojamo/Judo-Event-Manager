package au.com.jwatmuff.eventmanager.print;

import au.com.jwatmuff.eventmanager.model.vo.CompetitionInfo;
import au.com.jwatmuff.genericdb.Database;
import java.util.List;
import java.util.Map;
import org.apache.velocity.context.Context;

/**
 *
 * @author James
 */
public class ResultListHTMLGenerator extends VelocityHTMLGenerator {
    List<Map> results;
    Database database;
    public ResultListHTMLGenerator(Database database, List<Map> results) {
        this.results = results;
        this.database = database;
    }

    @Override
    public void populateContext(Context c) {
        c.put("results", results);
        c.put("competitionName", database.get(CompetitionInfo.class, null).getName());
    }

    @Override
    public String getTemplateName() {
        return "results.html";
    }
}

