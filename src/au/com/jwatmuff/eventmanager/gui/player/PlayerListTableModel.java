/*
 * PlayerListTableModel.java
 *
 * Created on 26 February 2008, 11:21
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.gui.player;

import au.com.jwatmuff.eventmanager.db.PlayerDAO;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.util.BeanTableModel;
import au.com.jwatmuff.eventmanager.util.DistributableBeanTableModel;
import au.com.jwatmuff.eventmanager.util.JexlBeanTableModel;
import au.com.jwatmuff.genericdb.Database;
import au.com.jwatmuff.genericdb.distributed.DataEvent;
import au.com.jwatmuff.genericdb.transaction.TransactionListener;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import org.apache.log4j.Logger;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author James
 */
public class PlayerListTableModel extends JexlBeanTableModel<Player> implements TransactionListener {
    private static Logger log = Logger.getLogger(PlayerListTableModel.class);
    
    private Database database;
    
    private BeanTableModel<Player> tableModel;
    
    /** Creates a new instance of PlayerListTableModel */
    public PlayerListTableModel(Database database, TransactionNotifier notifier) {
        //super(Player.class);
        super();
        notifier.addListener(this, Player.class);
        this.database = database;
        this.tableModel = new DistributableBeanTableModel<Player>(this);
    }

    public void updateTableFromDatabase() {
        setPlayers(database.findAll(Player.class, PlayerDAO.ALL));
    }
    
    private void setPlayers(Collection<Player> players) {
        tableModel.setBeans(players);
    }
    
    @Override
    public void handleTransactionEvents(List<DataEvent> events, Collection<Class> dataClasses) {
        updateTableFromDatabase();
    }
}
