/*
 * PlayerPoolInfo.java
 *
 * Created on 6 August 2008, 18:46
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.info;

import au.com.jwatmuff.eventmanager.db.PlayerPoolDAO;
import au.com.jwatmuff.eventmanager.model.vo.PlayerDetails;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.PlayerPool;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.genericdb.Database;
import java.util.Collection;
import java.util.ArrayList;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class PlayerPoolInfo {
    private static final Logger log = Logger.getLogger(PlayerPoolInfo.class);

    private Player player;
    private PlayerDetails playerDetails;
    private Pool pool;
    private PlayerPool playerPool;
    
    /** Creates a new instance of PlayerPoolInfo */
    public PlayerPoolInfo(Database database, int playerID, int poolID) {
        playerPool = database.get(PlayerPool.class, new PlayerPool.Key(playerID, poolID));
        pool = database.get(Pool.class, poolID);
        player = database.get(Player.class, playerID);
        playerDetails = database.get(PlayerDetails.class, player.getDetailsID());
    }
    
    public PlayerPoolInfo(Database database, PlayerPool pp) {
        this.playerPool = pp;
        pool = database.get(Pool.class, pp.getPoolID());
        player = database.get(Player.class, pp.getPlayerID());
        playerDetails = database.get(PlayerDetails.class, player.getDetailsID());
    }
    
    public PlayerPoolInfo(PlayerPool playerPool, Player player, PlayerDetails playerDetails, Pool pool) {
        this.playerPool = playerPool;
        this.player = player;
        this.playerDetails = playerDetails;
        this.pool = pool;
    }

    public static Collection<PlayerPoolInfo> getForPlayer(Database database, int playerID) {
        Collection<PlayerPoolInfo> playerPoolInfoList = new ArrayList<PlayerPoolInfo>();
        Player player = database.get(Player.class, playerID);
        PlayerDetails playerDetails = database.get(PlayerDetails.class, player.getDetailsID());

        for(PlayerPool playerPool : database.findAll(PlayerPool.class, PlayerPoolDAO.FOR_PLAYER, playerID)) {
            Pool pool = database.get(Pool.class, playerPool.getPoolID());
            playerPoolInfoList.add(new PlayerPoolInfo(playerPool, player, playerDetails, pool));
        }
        
        return playerPoolInfoList;
    }

    public static Collection<PlayerPoolInfo> getForPool(Database database, int poolID) {
        Collection<PlayerPoolInfo> playerPoolInfoList = new ArrayList<PlayerPoolInfo>();
        Pool pool = database.get(Pool.class, poolID);

        for(PlayerPool playerPool : database.findAll(PlayerPool.class, PlayerPoolDAO.FOR_POOL, poolID)) {
            Player player = database.get(Player.class, playerPool.getPlayerID());
            PlayerDetails playerDetails = database.get(PlayerDetails.class, player.getDetailsID());
            playerPoolInfoList.add(new PlayerPoolInfo(playerPool, player, playerDetails, pool));
        }
        
        return playerPoolInfoList;
    }

    
    public Player getPlayer() {
        return player;
    }

    public PlayerDetails getPlayerDetails() {
        return playerDetails;
    }

    public Pool getPool() {
        return pool;
    }

    public PlayerPool getPlayerPool() {
        return playerPool;
    }

    public boolean isWithdrawn() {
        return (playerPool.getStatus()==PlayerPool.Status.WITHDRAWN || playerPool.getStatus()==PlayerPool.Status.DISQUALIFIED);
    }
}
