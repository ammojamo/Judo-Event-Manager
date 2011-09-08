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
import java.util.Collections;
import java.util.Comparator;
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

    private static final Comparator<Player> PLAYERS_COMPARATOR = new Comparator<Player>() {
        @Override
        public int compare(Player p1, Player p2) {
            String n1 = p1.getLastName() + p1.getFirstName();
            String n2 = p2.getLastName() + p2.getFirstName();
            return n1.compareTo(n2);
        }
    };

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
            Collections.sort(poolPlayers, PLAYERS_COMPARATOR);
            if(poolPlayers.isEmpty())
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
