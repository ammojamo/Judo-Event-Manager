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

import au.com.jwatmuff.eventmanager.db.PlayerPoolDAO;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.PlayerPool;
import au.com.jwatmuff.eventmanager.util.ObjectCopier;
import au.com.jwatmuff.genericdb.transaction.Transaction;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import java.util.Collection;

/**
 *
 * @author James
 */
public class PlayerLocker {
    private PlayerLocker() {}
    
    public static Player lockPlayer(final TransactionalDatabase database, final Player p) throws DatabaseStateException {
        final Player player = database.get(Player.class, p.getID());
        if(player == null)
            throw new DatabaseStateException("Unable to lock player: not found in database");
        
        if(player.getLockedStatus() == Player.LockedStatus.LOCKED)
            throw new DatabaseStateException("Unable to lock player: player already locked");

        if(player.getWeight() == 0.0)
            throw new DatabaseStateException("Unable to lock player: recorded weight is zero");

        final Collection<PlayerPool> pps = database.findAll(PlayerPool.class, PlayerPoolDAO.FOR_PLAYER, player.getID());
        final Player lockedPlayer = Player.getLockedCopy(player, Player.LockedStatus.LOCKED);
        
        database.perform(new Transaction() {
            @Override
            public void perform() {
                database.delete(player);
                database.add(lockedPlayer);
                for(PlayerPool pp : pps) {
                    PlayerPool newpp = ObjectCopier.copy(pp);
                    newpp.setPlayerID(lockedPlayer.getID());
                    database.delete(pp);
                    database.add(newpp);
                }
            }
        });
        
        
        return lockedPlayer;
    }
}
