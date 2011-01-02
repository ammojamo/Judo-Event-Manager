/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.cache;

import au.com.jwatmuff.eventmanager.db.PlayerDAO;
import au.com.jwatmuff.eventmanager.db.FightDAO;
import au.com.jwatmuff.eventmanager.db.ResultDAO;
import au.com.jwatmuff.eventmanager.model.info.ResultInfo;
import au.com.jwatmuff.eventmanager.model.misc.DatabaseStateException;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser.FightPlayer;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser.PlayerType;
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
import java.util.Collection;
import java.util.Comparator;
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
    
    private List<PlayerScore> getScores(int poolID) {
        List<PlayerScore> scores = new ArrayList<PlayerScore>();
        Collection<Player> players = database.findAll(Player.class, PlayerDAO.FOR_POOL, poolID, true);
        Map<Integer, Integer> playerScores = new HashMap<Integer, Integer>();
        if(players.size() == 1) {
//        To handle when a single player is in a division with a locked draw
            Player player = players.iterator().next();
            int playerID = player.getID();
            playerScores.put(playerID, 1);
        } else {
//        Initialises all player names
            for (Player player : players) {
                int playerID = player.getID();
                if(!playerScores.containsKey(playerID))
                    playerScores.put(playerID, 0);
            }
        }

        for(Fight f : database.findAll(Fight.class, FightDAO.FOR_POOL, poolID)) {
            List<Result> results = database.findAll(Result.class, ResultDAO.FOR_FIGHT, f.getID());
            if(results.isEmpty()) continue;

            try {
                ResultInfo ri = riCache.getResultInfo(results.iterator().next().getID());
                for(int i = 0; i < 2; i++) {
                    Player player = ri.getPlayer()[i].player;
                    if(player == null) continue;
                    int playerID = player.getID();
                    if(!playerScores.containsKey(playerID))
                        playerScores.put(playerID, 0);
                    switch(ri.getResult().getSimpleScores()[i]) {
                        case 10:
                            playerScores.put(playerID, playerScores.get(playerID)+f.getPoints()[0]);
                            break;
                        case 7:
                            playerScores.put(playerID, playerScores.get(playerID)+f.getPoints()[1]);
                            break;
                        case 5:
                            playerScores.put(playerID, playerScores.get(playerID)+f.getPoints()[2]);
                            break;
                        case 1:
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

        if(scores.isEmpty()) return drs;
        
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
                PlayerScore score = scores.get(i++); //this is what is killing it
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

    public void filterDivisionsWithoutResults(List<Pool> divisions) {
        Iterator<Pool> iter = divisions.iterator();
        while(iter.hasNext()) {
            Pool division = iter.next();
            if(getDivisionResults(division.getID()).isEmpty())
                iter.remove();
        }
    }
}
