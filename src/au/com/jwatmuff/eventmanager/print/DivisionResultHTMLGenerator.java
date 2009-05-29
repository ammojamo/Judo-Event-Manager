package au.com.jwatmuff.eventmanager.print;

import au.com.jwatmuff.eventmanager.db.PoolDAO;
import au.com.jwatmuff.eventmanager.model.cache.DivisionResultCache;
import au.com.jwatmuff.eventmanager.model.cache.DivisionResultCache.DivisionResult;
import au.com.jwatmuff.eventmanager.model.vo.CompetitionInfo;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.genericdb.Database;
import java.util.HashMap;
import java.util.Iterator;
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
    public DivisionResultHTMLGenerator(Database database, DivisionResultCache cache) {
        this.database = database;
        this.cache = cache;
    }

    @Override
    public String getTemplateName() {
        return "divresults.html";
    }

    @Override
    public void populateContext(Context c) {
        Map<Integer, List<DivisionResult>> results = new HashMap<Integer, List<DivisionResult>>();
        List<Pool> pools = database.findAll(Pool.class, PoolDAO.WITH_LOCKED_STATUS, Pool.LockedStatus.FIGHTS_LOCKED);

        Iterator<Pool> iter = pools.iterator();
        while(iter.hasNext()) {
            Pool pool = iter.next();
            List<DivisionResult> drs = cache.getDivisionResults(pool.getID());
            if(drs.isEmpty()) {
                iter.remove();
            } else {
                results.put(pool.getID(), drs);
            }
        }

        c.put("pools", pools);
        c.put("results", results);

        c.put("competitionName", database.get(CompetitionInfo.class, 0).getName());
    }

}
