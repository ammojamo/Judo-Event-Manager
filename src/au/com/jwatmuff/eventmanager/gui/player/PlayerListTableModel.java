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

package au.com.jwatmuff.eventmanager.gui.player;

import au.com.jwatmuff.eventmanager.db.PlayerDAO;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.util.BeanTableModel;
import au.com.jwatmuff.eventmanager.util.DistributableBeanTableModel;
import au.com.jwatmuff.eventmanager.util.JexlBeanTableModel;
import au.com.jwatmuff.genericdb.Database;
import au.com.jwatmuff.genericdb.distributed.DataEvent;
import au.com.jwatmuff.genericdb.transaction.TransactionListener;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class PlayerListTableModel extends JexlBeanTableModel<Player> implements TransactionListener {
    private static Logger log = Logger.getLogger(PlayerListTableModel.class);
    
    private Database database;
    
    private BeanTableModel<Player> tableModel;
    private Pool fromDivision;
    private String nameStartingWith;
    
    /** Creates a new instance of PlayerListTableModel */
    public PlayerListTableModel(Database database, TransactionNotifier notifier) {
        //super(Player.class);
        super();
        notifier.addListener(this, Player.class);
        this.database = database;
        this.tableModel = new DistributableBeanTableModel<>(this);
    }

    public void updateTableFromDatabase() {
        List<Player> players;

        // only fetch players in selected division
        if(fromDivision == null || fromDivision.getID() == -1) {
            players = database.findAll(Player.class, PlayerDAO.ALL);
        } else {
            players = database.findAll(Player.class, PlayerDAO.FOR_POOL, fromDivision.getID(), true);
            players.addAll(database.findAll(Player.class, PlayerDAO.FOR_POOL, fromDivision.getID(), false));
        }

        // filter out name based on search
        if(!StringUtils.isEmpty(nameStartingWith)) {
            Iterator<Player> iter = players.iterator();
            while(iter.hasNext()) {
                Player player = iter.next();
                if(!player.getLastName().toLowerCase().startsWith(nameStartingWith.toLowerCase()) &&
                   !player.getFirstName().toLowerCase().startsWith(nameStartingWith.toLowerCase())) {
                    iter.remove();
                }
            }
        }

        setPlayers(players);
    }

    private void setPlayers(Collection<Player> players) {
        tableModel.setBeans(players);
    }
    
    public void setDivisionFilter(Pool division) {
        this.fromDivision = division;
        updateTableFromDatabase();
    }

    public void setNameFilter(String name) {
        this.nameStartingWith = name;
        updateTableFromDatabase();
    }

    @Override
    public void handleTransactionEvents(List<DataEvent> events, Collection<Class> dataClasses) {
        updateTableFromDatabase();
    }
}
