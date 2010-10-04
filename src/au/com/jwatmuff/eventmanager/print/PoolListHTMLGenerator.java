/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.print;

import au.com.jwatmuff.eventmanager.db.PlayerDAO;
import au.com.jwatmuff.eventmanager.db.PoolDAO;
import au.com.jwatmuff.eventmanager.model.vo.CompetitionInfo;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.model.vo.Pool.LockedStatus;
import au.com.jwatmuff.genericdb.Database;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.velocity.context.Context;

/**
 *
 * @author James
 */
public class PoolListHTMLGenerator extends VelocityHTMLGenerator {
    private Database database;
    private List<Pool> pools;

    public PoolListHTMLGenerator(Database database) {
        this(database, database.findAll(Pool.class, PoolDAO.ALL));
    }

    public PoolListHTMLGenerator(Database database, List<Pool> pools) {
        this.database = database;
        this.pools = new ArrayList<Pool>(pools);
    }

    @Override
    public void populateContext(Context c) {
        c.put("pools", pools);

        Map<Integer, List<Player>> players = new HashMap<Integer, List<Player>>();
        Iterator<Pool> iter = pools.iterator();
        while(iter.hasNext()) {
            Pool pool = iter.next();
            List<Player> poolPlayers = database.findAll(Player.class, PlayerDAO.FOR_POOL, pool.getID(), true);
            if(pool.getLockedStatus()==LockedStatus.UNLOCKED){
                poolPlayers.addAll(database.findAll(Player.class, PlayerDAO.FOR_POOL, pool.getID(), false));
            }
            if(poolPlayers.size() == 0)
                iter.remove();
            else
                players.put(pool.getID(), poolPlayers);
        }
        c.put("players", players);

        c.put("competitionName", database.get(CompetitionInfo.class, null).getName());
    }

    @Override
    public String getTemplateName() {
        return "pools.html";
    }
}
