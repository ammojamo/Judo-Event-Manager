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
        this.divisions = new ArrayList<Pool>(divisions);
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

        c.put("competitionName", database.get(CompetitionInfo.class, null).getName());
    }

}
