/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.cache;

import au.com.jwatmuff.eventmanager.db.FightDAO;
import au.com.jwatmuff.eventmanager.db.ResultDAO;
import au.com.jwatmuff.eventmanager.model.info.ResultInfo;
import au.com.jwatmuff.eventmanager.model.misc.DatabaseStateException;
import au.com.jwatmuff.eventmanager.model.vo.Fight;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.PlayerDetails;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.model.vo.Result;
import au.com.jwatmuff.genericdb.distributed.DataEvent;
import au.com.jwatmuff.genericdb.distributed.DataEventListener;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class DivisionResultCache {
    private static final Logger log = Logger.getLogger(DivisionResultCache.class);

    private Map<Integer, List<DivisionResult>> cache = new HashMap<Integer, List<DivisionResult>>();
    private TransactionalDatabase database;
    private ResultInfoCache riCache;
    private TransactionNotifier notifier;

    public class DivisionResult {
        public Pool pool;
        public Player player;
        public int place;

        // for velocity
        public Pool getPool() { return pool; }
        public Player getPlayer() { return player; }
        public int getPlace() { return place; }
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
    
    public DivisionResultCache(final TransactionalDatabase database, ResultInfoCache riCache, TransactionNotifier notifier) {
        this.notifier = notifier;
        this.database = database;
        this.riCache = riCache;
        notifier.addListener(listener, Result.class);
    }
    
    class PlayerScore {
        int playerID;
        int score;
    }
    
    private boolean fightsCompleted(int poolID) {
        for(Fight f : database.findAll(Fight.class, FightDAO.FOR_POOL, poolID))
            if(database.findAll(Result.class, ResultDAO.FOR_FIGHT, f.getID()).size() == 0)
                return false;

        return true;
    }
    
    private List<PlayerScore> getScores(int poolID) {
        List<PlayerScore> scores = new ArrayList<PlayerScore>();
        
        Map<Integer, Integer> playerScores = new HashMap<Integer, Integer>();
        
        for(Fight f : database.findAll(Fight.class, FightDAO.FOR_POOL, poolID)) {
            List<Result> results = database.findAll(Result.class, ResultDAO.FOR_FIGHT, f.getID());
            if(results.size() == 0) continue;

            try {
                ResultInfo ri = riCache.getResultInfo(results.iterator().next().getID());
                for(int i = 0; i < 2; i++) {
                    Player player = ri.getPlayer()[i].player;
                    if(player == null) continue;
                    int playerID = player.getID();
                    if(!playerScores.containsKey(playerID))
                        playerScores.put(playerID, 0);
                    switch(ri.getResult().getPlayerScores()[i]) {
                        case 10:
                            playerScores.put(playerID, playerScores.get(playerID)+f.getPoints()[0]);
                            break;
                        case 7:
                            playerScores.put(playerID, playerScores.get(playerID)+f.getPoints()[1]);
                            break;
                        case 5:
                            playerScores.put(playerID, playerScores.get(playerID)+f.getPoints()[2]);
                            break;
                        case 3:
                            playerScores.put(playerID, playerScores.get(playerID)+f.getPoints()[3]);
                            break;
                        default:
                            playerScores.put(playerID, playerScores.get(playerID)+f.getPoints()[4]);
                            break;
                    }
                }
            } catch(DatabaseStateException e) {
                log.error("Error calculating division scores", e);
            }
        }
        
        for(Integer playerID : playerScores.keySet()) {
            PlayerScore score = new PlayerScore();
            score.playerID = playerID;
            score.score = playerScores.get(playerID);
            scores.add(score);
        }
        
        return scores;
    }
    
    public List<DivisionResult> getDivisionResults(int poolID) {
        List<DivisionResult> drs = cache.get(poolID);
        if(drs != null) return drs;
        
        drs = new ArrayList<DivisionResult>();

        if(!fightsCompleted(poolID)) return drs;
        
        List<PlayerScore> scores = getScores(poolID);
        
        Collections.sort(scores, new Comparator<PlayerScore>(){
            @Override
            public int compare(PlayerScore p0, PlayerScore p1) {
                return p1.score - p0.score; //reverse order deliberate
            }
        });
        
        Pool pool = database.get(Pool.class, poolID);
        
        int i = 0;
        boolean done = false;
        for(int place = 1; place <= 3; place++) {
            while(true) {
                PlayerScore score = scores.get(i++);
                if(score.score == 0) { done = true; break; }
                DivisionResult dr = new DivisionResult();
                dr.place = place;
                dr.pool = pool;
                dr.player = database.get(Player.class, score.playerID);
                drs.add(dr);
                if(i == scores.size()) { done = true; break; }
                if(scores.get(i).score != score.score) break;
            }
            if(done) break;
        }

        cache.put(poolID, drs);
        return drs;
    }

    public void shutdown() {
        notifier.removeListener(listener);
    }
}
