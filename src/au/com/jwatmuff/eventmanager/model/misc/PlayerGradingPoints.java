/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.misc;

import au.com.jwatmuff.eventmanager.db.FightDAO;
import au.com.jwatmuff.eventmanager.db.ResultDAO;
import au.com.jwatmuff.eventmanager.model.cache.ResultInfoCache;
import au.com.jwatmuff.eventmanager.model.info.ResultInfo;
import au.com.jwatmuff.eventmanager.model.vo.Fight;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.Result;
import au.com.jwatmuff.genericdb.Database;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class PlayerGradingPoints {
    private static final Logger log = Logger.getLogger(PlayerGradingPoints.class);
    public Player player;
    public List<FightGradingPoints> fights;

    public PlayerGradingPoints(Player player) {
        this.player = player;
        this.fights = new ArrayList<FightGradingPoints>();
    }

    public Player getPlayer() {
        return player;
    }

    public List<FightGradingPoints> getFights() {
        return fights;
    }
    
    public static List<PlayerGradingPoints> getAllPlayerGradingPoints(ResultInfoCache cache, Database database) {
        Map<Integer, PlayerGradingPoints> playerPoints = new HashMap<Integer, PlayerGradingPoints>();

        /* for all fights with results */
        for(Fight f : database.findAll(Fight.class, FightDAO.WITH_RESULT)) {
            /* get the most recent result */
            Result r = database.findAll(Result.class, ResultDAO.FOR_FIGHT, f.getID()).iterator().next();
            try {
                /* ignore bye fights */
                if(r.getPlayerIDs()[0] <= 0 || r.getPlayerIDs()[1] <= 0) continue;

                /* get result info and calculate points */
                ResultInfo ri = cache.getResultInfo(r.getID());
                FightGradingPoints points = new FightGradingPoints(ri, database);

                /* get player grading points object for the winning player */
                Player winner = points.winningPlayer;
                if(!playerPoints.containsKey(winner.getID())) {
                    playerPoints.put(winner.getID(), new PlayerGradingPoints(winner));
                }
                PlayerGradingPoints pgp = playerPoints.get(winner.getID());

                /* add the points from this fight to the player grading points */
                pgp.fights.add(points);

            } catch(Exception e) {
                log.error(e, e);
            }
        }
        return new ArrayList<PlayerGradingPoints>(playerPoints.values());
    }
}
