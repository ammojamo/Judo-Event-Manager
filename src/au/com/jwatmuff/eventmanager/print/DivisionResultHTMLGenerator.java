package au.com.jwatmuff.eventmanager.print;

import au.com.jwatmuff.eventmanager.db.PoolDAO;
import au.com.jwatmuff.eventmanager.model.cache.DivisionResultCache;
import au.com.jwatmuff.eventmanager.model.cache.DivisionResultCache.DivisionResult;
import au.com.jwatmuff.eventmanager.model.vo.CompetitionInfo;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.genericdb.Database;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.velocity.context.Context;

/**
 *
 * @author James
 */
public class DivisionResultHTMLGenerator extends VelocityHTMLGenerator {
    DivisionResultCache cache;
    Database database;
    List<Pool> divisions;

    public DivisionResultHTMLGenerator(Database database, DivisionResultCache cache, List<Pool> divisions)
    {
        this.database = database;
        this.cache = cache;
        this.divisions = new ArrayList(divisions);
    }

    public DivisionResultHTMLGenerator(Database database, DivisionResultCache cache) {
        this(database, cache, database.findAll(Pool.class, PoolDAO.WITH_LOCKED_STATUS, Pool.LockedStatus.FIGHTS_LOCKED));
    }

    @Override
    public String getTemplateName() {
        return "divresults.html";
    }

    @Override
    public void populateContext(Context c) {
        Map<Integer, List<DivisionResult>> results = new HashMap<Integer, List<DivisionResult>>();
        cache.filterDivisionsWithoutResults(divisions);
        for(Pool division : divisions) {
            List<DivisionResult> drs = cache.getDivisionResults(division.getID());
            assert(!drs.isEmpty());
            results.put(division.getID(), drs);
        }

        c.put("pools", divisions);
        c.put("results", results);

        c.put("competitionName", database.get(CompetitionInfo.class, 0).getName());
    }

}
