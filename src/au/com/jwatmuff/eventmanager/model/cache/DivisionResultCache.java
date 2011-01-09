/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.cache;

import au.com.jwatmuff.eventmanager.db.FightDAO;
import au.com.jwatmuff.eventmanager.db.ResultDAO;
import au.com.jwatmuff.eventmanager.model.misc.DatabaseStateException;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser.FightPlayer;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser.PlayerType;
import au.com.jwatmuff.eventmanager.model.vo.Fight;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.PlayerDetails;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.model.vo.Pool.Place;
import au.com.jwatmuff.eventmanager.model.vo.Result;
import au.com.jwatmuff.genericdb.distributed.DataEvent;
import au.com.jwatmuff.genericdb.distributed.DataEventListener;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class DivisionResultCache {
    private static final Logger log = Logger.getLogger(DivisionResultCache.class);

    private Map<Integer, List<DivisionResult>> cache = new HashMap<Integer, List<DivisionResult>>();
    private TransactionalDatabase database;
    private TransactionNotifier notifier;

    public class DivisionResult {
        public Pool pool;
        public Player player;
        public String place;

        // for velocity
        public Pool getPool() { return pool; }
        public Player getPlayer() { return player; }
        public String getPlace() { return place; }
        public PlayerDetails getDetails() {
            log.debug("Fetching player details");
            return database.get(PlayerDetails.class, player.getDetailsID());
        }
    }

    DataEventListener listener = new DataEventListener() {
        @Override
        public void handleDataEvent(DataEvent event) {
            Result r = (Result)event.getData();
            Fight f = database.get(Fight.class, r.getFightID());
            cache.remove(f.getPoolID());
        }
    };
    
    public DivisionResultCache(final TransactionalDatabase database, TransactionNotifier notifier) {
        this.notifier = notifier;
        this.database = database;
        notifier.addListener(listener, Result.class);
    }
    
    class PlayerScore {
        int playerID;
        int score;
    }
    
    private boolean fightsCompleted(int poolID) {
        boolean isBye;
        for(Fight f : database.findAll(Fight.class, FightDAO.FOR_POOL, poolID)) {
            if(database.findAll(Result.class, ResultDAO.FOR_FIGHT, f.getID()).isEmpty()) {
                isBye = false;
                for(int i = 0; i < 2; i++) {
                    String code = f.getPlayerCodes()[i];
                    FightPlayer fp;
// TODO: Leonard: Why do I need this try catch when I didin't need it before, I also needed the import bla.misc.PlayerCodeParser bit when I didn't need it before
                    try {
                        fp = PlayerCodeParser.parseCode(database, code, f.getPoolID());
                        if(fp.type == PlayerType.BYE){
                            isBye = true;
                        }
                    } catch (DatabaseStateException ex) {
                        java.util.logging.Logger.getLogger(DivisionResultCache.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                if(!isBye) {
                    return false;
                }
//                return false;
            }
        }
        return true;
    }

    public List<DivisionResult> getDivisionResults(int poolID) {
        List<DivisionResult> drs = cache.get(poolID);
        if(drs != null) return drs;
        
        drs = new ArrayList<DivisionResult>();

        if(!fightsCompleted(poolID)) return drs;

        Pool pool = database.get(Pool.class, poolID);

        for(Place place : pool.getPlaces()) {
            try {
                DivisionResult result = new DivisionResult();
                result.place = place.name;
                result.pool = pool;
                result.player = PlayerCodeParser.parseCode(database, place.code, poolID).player;
                drs.add(result);
            } catch(DatabaseStateException e) {
                log.error("Error while calculating division result", e);
            }
        }

        cache.put(poolID, drs);
        return drs;
    }

    public void shutdown() {
        notifier.removeListener(listener);
    }

    public void filterDivisionsWithoutResults(List<Pool> divisions) {
        Iterator<Pool> iter = divisions.iterator();
        while(iter.hasNext()) {
            Pool division = iter.next();
            if(getDivisionResults(division.getID()).isEmpty())
                iter.remove();
        }
    }
}
