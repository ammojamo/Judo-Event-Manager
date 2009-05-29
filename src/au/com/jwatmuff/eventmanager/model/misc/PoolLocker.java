/*
 * PoolLocker.java
 *
 * Created on 13 August 2008, 16:46
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.misc;

import au.com.jwatmuff.eventmanager.db.FightDAO;
import au.com.jwatmuff.eventmanager.db.PlayerDAO;
import au.com.jwatmuff.eventmanager.db.PlayerPoolDAO;
import au.com.jwatmuff.eventmanager.model.vo.Fight;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.PlayerPool;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.util.ObjectCopier;
import au.com.jwatmuff.genericdb.transaction.Transaction;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class PoolLocker {
    private static final Logger log = Logger.getLogger(PoolLocker.class);
    
    /** Creates a new instance of PoolLocker */
    private PoolLocker() {
    }
    
    /**
     * Locks the current set of approved players into this pool. A new
     * player-locked pool is created with the set of locked players, and the
     * old (unlocked) pool is marked as invalid.
     * 
     * @param database  The database to update
     * @param pool      The pool to be locked
     * @return          The new locked pool
     * @throws au.com.jwatmuff.eventmanager.model.misc.DatabaseStateException if
     * the pool is not able to be locked, for example if the pool has already
     * been locked perviously.
     */
    public static Pool lockPoolPlayers(final TransactionalDatabase database, final Pool pool) throws DatabaseStateException {
        assert database != null;
        assert pool != null;

        log.debug("Attempting to lock players in pool " + pool.getDescription() + " (" + pool.getID() + ")");

        if(pool.getLockedStatus() != Pool.LockedStatus.UNLOCKED)
            throw new DatabaseStateException("The players in this pool have already been locked");

        Collection<Player> players = database.findAll(Player.class, PlayerDAO.FOR_POOL, pool.getID(), true);
        for(Player player : players)
            if(player.getLockedStatus() != Player.LockedStatus.LOCKED)
                throw new DatabaseStateException("One or more players in this pool have not been locked using the Weigh-in interface");

        final Collection<PlayerPool> pps = database.findAll(PlayerPool.class, PlayerPoolDAO.FOR_POOL, pool.getID());
        
        final Pool lockedPool = Pool.getLockedCopy(pool, Pool.LockedStatus.PLAYERS_LOCKED);
        
        database.perform(new Transaction() {

            @Override
            public void perform() {
                int i = 1;

                database.add(lockedPool);

                for(PlayerPool pp : pps) {
                    PlayerPool newpp = ObjectCopier.copy(pp);
                    newpp.setPoolID(lockedPool.getID());
                    if(newpp.isApproved())
                        newpp.setPlayerPosition(i++);
                    database.add(newpp);
                    database.delete(pp);
                }

                database.delete(pool);
               
            }

        });
                
        log.debug("Pool locked");
        
        return lockedPool;
    }

    /**
     * Locks the current set of fights into this pool. A new fight-locked
     * pool is created with the set of locked fights and players, and the old
     * pool is marked as invalid.
     * 
     * @param database  The database to update
     * @param pool      The pool to be locked
     * @return          The new locked pool
     * @throws au.com.jwatmuff.eventmanager.model.misc.DatabaseStateException if
     * the pool is not able to be fight-locked, for example if the pool has
     * already been fight-locked perviously, or players have not yet been
     * locked into this pool, or there are no fights to be locked.
     */
    public static Pool lockPoolFights(final TransactionalDatabase database, Pool p) throws DatabaseStateException {
        assert p != null;
        assert database != null;

        final Pool pool = database.get(Pool.class, p.getID());
        if(pool == null)
            throw new DatabaseStateException("Pool does not exist in database");
        if(pool.getLockedStatus() == Pool.LockedStatus.FIGHTS_LOCKED)
            throw new DatabaseStateException("Fights have already been locked for this pool");
        if(pool.getLockedStatus() != Pool.LockedStatus.PLAYERS_LOCKED)
            throw new DatabaseStateException("Pool players must be already locked");
        
        final Collection<Fight> fights = database.findAll(Fight.class, FightDAO.FOR_POOL, pool.getID());
        if(fights.size() < 1)
            throw new DatabaseStateException("No fights in this pool to be locked");

        /* ensure all players in pool have a fight */
/*
        int players = database.findAll(Player.class, PlayerDAO.FOR_POOL, true).size();
        Set<String> playerCodes = new HashSet<String>();
        for(int i = 1; i <= players; i++) {
            playerCodes.add("P" + i);
        }
        for(Fight f : fights) {
            playerCodes.remove(f.getPlayerCodes()[0]);
            playerCodes.remove(f.getPlayerCodes()[1]);
        }
        if(!playerCodes.isEmpty())
            throw new DatabaseStateException("Not all players in this pool have been assigned to fights");
*/

        final Pool lockedPool = Pool.getLockedCopy(pool, Pool.LockedStatus.FIGHTS_LOCKED);
        
        database.perform(new Transaction() {

            @Override
            public void perform() {
                database.add(lockedPool);

                for(Fight fight : fights) {
                    Fight lockedFight = Fight.getLockedCopy(fight, lockedPool);
                    database.add(lockedFight);
                }

                Collection<PlayerPool> pps = database.findAll(PlayerPool.class, PlayerPoolDAO.FOR_POOL, pool.getID());
                for(PlayerPool pp : pps) {
                    PlayerPool lockedpp = PlayerPool.getLockedCopy(pp, lockedPool);
                    database.add(lockedpp);
                }

                database.delete(pool);

            }            
        });

        
        return lockedPool;
    }
}
