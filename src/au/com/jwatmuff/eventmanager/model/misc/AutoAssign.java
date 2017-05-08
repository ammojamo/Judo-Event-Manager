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

package au.com.jwatmuff.eventmanager.model.misc;

import au.com.jwatmuff.eventmanager.db.PlayerDAO;
import au.com.jwatmuff.eventmanager.db.PoolDAO;
import au.com.jwatmuff.eventmanager.model.config.ConfigurationFile;
import au.com.jwatmuff.eventmanager.model.info.PlayerPoolInfo;
import au.com.jwatmuff.eventmanager.model.vo.CompetitionInfo;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.PlayerPool;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.genericdb.transaction.Transaction;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import java.util.Collection;

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
        ConfigurationFile configurationFile = ConfigurationFile.getConfiguration(database.get(CompetitionInfo.class, null).getDrawConfiguration());

            @Override
            public void perform() {
                for(Player player : players)
                    for(Pool pool : pools)
                        if(pool.getLockedStatus() == Pool.LockedStatus.UNLOCKED)
                            if(PoolChecker.checkPlayer(player, pool, ci.getAgeThresholdDate(), configurationFile)) {
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
        final ConfigurationFile configurationFile = ConfigurationFile.getConfiguration(database.get(CompetitionInfo.class, null).getDrawConfiguration());
        if(pool.getLockedStatus() != Pool.LockedStatus.UNLOCKED)
            throw new DatabaseStateException("Cannot approve players in a locked pool");
        
        database.perform(new Transaction() {
            @Override
            public void perform() {
                CompetitionInfo ci = database.get(CompetitionInfo.class, null);
                for(PlayerPoolInfo ppi : PlayerPoolInfo.getForPool(database, pool.getID())) {
                    if(PoolChecker.checkPlayer(ppi.getPlayer(), ppi.getPool(), ci.getAgeThresholdDate(), configurationFile)) {
                        PlayerPool pp = ppi.getPlayerPool();
                        pp.setApproved(true);
                        database.update(pp);
                    }
                }
            }
        });
    }
}
