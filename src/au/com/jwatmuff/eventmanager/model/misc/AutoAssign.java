/*
 * AutoAssign.java
 *
 * Created on 31 July 2008, 12:19
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.misc;

import au.com.jwatmuff.eventmanager.db.PlayerDAO;
import au.com.jwatmuff.eventmanager.db.PoolDAO;
import au.com.jwatmuff.eventmanager.model.info.PlayerPoolInfo;
import au.com.jwatmuff.eventmanager.model.vo.CompetitionInfo;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.PlayerPool;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.genericdb.transaction.Transaction;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import java.util.Collection;
import java.util.Date;

/**
 *
 * @author James
 */
public class AutoAssign {
    
    /** Creates a new instance of AutoAssign */
    private AutoAssign() {
    }
    
    /**
     * Adds each player to all the pools they are eligible to be entered into,
     * if they have not already entered. For evaluating age requirements, the
     * ages of players is calculated as of the starting date of the competition.
     * 
     * @param database The database to update
     * @throws au.com.jwatmuff.eventmanager.model.misc.DatabaseStateException if
     * a competition start date could not be found in the database.
     */
    public static void assignPlayersToPools(final TransactionalDatabase database) throws DatabaseStateException {
        final CompetitionInfo ci = database.get(CompetitionInfo.class, null);
        final Collection<Player> players = database.findAll(Player.class, PlayerDAO.ALL);
        final Collection<Pool> pools = database.findAll(Pool.class, PoolDAO.ALL);
        
        if(ci == null)
            throw new DatabaseStateException("Competition Info must be present");
        if(ci.getAgeThresholdDate() == null)
            throw new DatabaseStateException("Competition Info must have an age threshold date");
        
        database.perform(new Transaction() {

            @Override
            public void perform() {
                for(Player player : players)
                    for(Pool pool : pools)
                        if(pool.getLockedStatus() == Pool.LockedStatus.UNLOCKED)
                            if(PoolChecker.checkPlayer(player, pool, ci.getAgeThresholdDate())) {
                                PlayerPool pp = new PlayerPool();
                                pp.setPlayerID(player.getID());
                                pp.setPoolID(pool.getID());
                                if(database.get(PlayerPool.class, new PlayerPool.Key(player.getID(), pool.getID())) != null)
                                    database.update(pp);
                                else
                                    database.add(pp);
                            }
            }
            
        });
    }
    
    public static void autoApprovePlayers(final TransactionalDatabase database, final Pool pool) throws DatabaseStateException {
        if(pool.getLockedStatus() != Pool.LockedStatus.UNLOCKED)
            throw new DatabaseStateException("Cannot approve players in a locked pool");
        
        database.perform(new Transaction() {
            @Override
            public void perform() {
                for(PlayerPoolInfo ppi : PlayerPoolInfo.getForPool(database, pool.getID())) {
                    if(PoolChecker.checkPlayer(ppi.getPlayer(), ppi.getPool(), new Date())) {
                        PlayerPool pp = ppi.getPlayerPool();
                        pp.setApproved(true);
                        database.update(pp);
                    }
                }
            }
        });
    }
}
