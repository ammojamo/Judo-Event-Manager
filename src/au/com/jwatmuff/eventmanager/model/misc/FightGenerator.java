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

import au.com.jwatmuff.eventmanager.db.FightDAO;
import au.com.jwatmuff.eventmanager.db.PlayerDAO;
import au.com.jwatmuff.eventmanager.model.vo.Fight;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.genericdb.transaction.Transaction;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class FightGenerator {
    private static final Logger log = Logger.getLogger(FightGenerator.class);
    
    /** Creates a new instance of FightGenerator */
    private FightGenerator() {
    }
    
    /**
     * Generates a simple elimination fight draw for players in a given pool.
     * 
     * @param database The competition database
     * @param pool     The pool for which fights are to be generated
     * @throws au.com.jwatmuff.eventmanager.model.misc.DatabaseStateException if
     * the requested pool is not in a valid state for assigning new fights, for
     * example if the players in the pool have not been locked, or if fights
     * have already been assigned and locked.
     */
    public static void generateFightsForPool(final TransactionalDatabase database, Pool p) throws DatabaseStateException {
        log.debug("Generating fights for pool " + p.getDescription());
        // refresh pool object from database
        final Pool pool = database.get(Pool.class, p.getID());
        
        if(pool.getLockedStatus() == Pool.LockedStatus.UNLOCKED)
            throw new DatabaseStateException("Cannot generate fights for unlocked pool");
        if(pool.getLockedStatus() == Pool.LockedStatus.FIGHTS_LOCKED)
            throw new DatabaseStateException("Fights are already locked for this pool");
        
        // remove any previously defined fights for this pool
        Collection<Fight> oldFights = database.findAll(Fight.class, FightDAO.FOR_POOL, pool.getID());
        for(Fight fight : oldFights)
            database.delete(fight);
        
        Collection<Player> players = database.findAll(Player.class, PlayerDAO.FOR_POOL, pool.getID(), true);
        log.debug("Number of players: " + players.size());
        
        if(players.size() >= 2) {
            final Collection<Fight> fights = generateEliminationFights(players);

            database.perform(new Transaction() {

                @Override
                public void perform() {
                    for(Fight fight : fights) {
                        fight.setPoolID(pool.getID());
                        database.add(fight);
                    }
                }
                
            });
                    
            log.debug("Generated fights for pool");
        } else {
            log.debug("No fights generated due to insufficient players");
        }
    }
    
    private static Collection<Fight> generateEliminationFights(Collection<Player> players) {
        Collection<Fight> fights = new ArrayList<Fight>();
        int numPlayers = players.size();
        
        // work out player codes
        List<String> codes = new ArrayList<String>();
        for(int i=0;i<numPlayers; i++)
            codes.add("P" + (i+1));
        for(int i=0; i<numPlayers-2; i++)
            codes.add("W" + (i+1));
        
        // populate list of fights
        for(int i=0; i<numPlayers-1; i++) {
            Fight fight = new Fight();
            fight.setPlayerCodes(new String[] { codes.get(i*2), codes.get(i*2+1) });
            fight.setPosition(i+1);
            fights.add(fight);
        }
        
        return fights;
    }    
}
