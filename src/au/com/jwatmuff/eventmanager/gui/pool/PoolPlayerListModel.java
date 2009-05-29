/*
 * PoolPlayerListModel.java
 *
 * Created on 28 April 2008, 11:06
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package au.com.jwatmuff.eventmanager.gui.pool;

import au.com.jwatmuff.eventmanager.db.PlayerDAO;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.PlayerPool;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.genericdb.Database;
import au.com.jwatmuff.genericdb.distributed.DataEvent;
import au.com.jwatmuff.genericdb.transaction.TransactionListener;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListModel;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class PoolPlayerListModel extends DefaultListModel implements TransactionListener {
    private static final Logger log = Logger.getLogger(PoolPlayerListModel.class);
    private Database database;
    private boolean approved;
    private Pool selectedPool = null;

    /** Creates a new instance of PoolPlayerListModel */
    public PoolPlayerListModel(Database database, TransactionNotifier notifier, boolean approved) {
        this.database = database;
        this.approved = approved;
        notifier.addListener(this, Pool.class, Player.class, PlayerPool.class);
    }

    private void updateFromDatabase() {
        this.removeAllElements();
        if (selectedPool != null) {
            Collection<Player> players;
            if (selectedPool.getID() == 0) {
                if (approved) {
                    players = database.findAll(Player.class, PlayerDAO.WITHOUT_POOL);
                } else {
                    players = new ArrayList<Player>();
                }
            } else {
                players = database.findAll(Player.class, PlayerDAO.FOR_POOL, selectedPool.getID(), approved);
            }
            for (Player player : players) {
                this.addElement(player);
            }
        }
    }

    public void setSelectedPool(Pool selectedPool) {
        this.selectedPool = selectedPool;
        updateFromDatabase();
    }

    public Player getPlayerAt(int i) {
        //return ((PlayerString) getElementAt(i)).player;
        return (Player) getElementAt(i);
    }

    @Override
    public void handleTransactionEvents(List<DataEvent> events, Collection<Class> dataClasses) {
        updateFromDatabase();
    }
}
