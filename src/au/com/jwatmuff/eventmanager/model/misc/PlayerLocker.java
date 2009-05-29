/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
