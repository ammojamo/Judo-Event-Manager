/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.print;

import au.com.jwatmuff.eventmanager.model.cache.ResultInfoCache;
import au.com.jwatmuff.eventmanager.model.misc.PlayerGradingPoints;
import au.com.jwatmuff.eventmanager.model.vo.CompetitionInfo;
import au.com.jwatmuff.genericdb.Database;
import org.apache.velocity.context.Context;

/**
 *
 * @author James
 */
public class GradingPointsHTMLGenerator extends VelocityHTMLGenerator {
    private Database database;
    private ResultInfoCache cache;

    public GradingPointsHTMLGenerator(Database database, ResultInfoCache cache) {
        this.database = database;
        this.cache = cache;
    }

    @Override
    public void populateContext(Context c) {
        c.put("players", PlayerGradingPoints.getAllPlayerGradingPoints(cache, database));
        c.put("competitionName", database.get(CompetitionInfo.class, null).getName());
    }

    @Override
    public String getTemplateName() {
        return "gradingpoints.html";
    }
}
