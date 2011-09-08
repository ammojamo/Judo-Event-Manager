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
import java.util.Collections;
import java.util.Comparator;
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

    private static final Comparator<Player> PLAYERS_COMPARATOR = new Comparator<Player>() {
        @Override
        public int compare(Player p1, Player p2) {
            String n1 = p1.getLastName() + p1.getFirstName();
            String n2 = p2.getLastName() + p2.getFirstName();
            return n1.compareTo(n2);
        }
    };

    /** Creates a new instance of PoolPlayerListModel */
    public PoolPlayerListModel(Database database, TransactionNotifier notifier, boolean approved) {
        this.database = database;
        this.approved = approved;
        notifier.addListener(this, Pool.class, Player.class, PlayerPool.class);
    }

    private void updateFromDatabase() {
        this.removeAllElements();
        if (selectedPool != null) {
            List<Player> players;
            if (selectedPool.getID() == 0) {
                if (approved) {
                    players = database.findAll(Player.class, PlayerDAO.WITHOUT_POOL);
                } else {
                    players = new ArrayList<Player>();
                }
            } else {
                players = database.findAll(Player.class, PlayerDAO.FOR_POOL, selectedPool.getID(), approved);
            }
            Collections.sort(players, PLAYERS_COMPARATOR);
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
