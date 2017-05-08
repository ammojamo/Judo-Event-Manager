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

package au.com.jwatmuff.eventmanager.gui.draw;

import au.com.jwatmuff.eventmanager.model.info.PlayerPoolInfo;
import au.com.jwatmuff.eventmanager.model.misc.DatabaseStateException;
import au.com.jwatmuff.eventmanager.model.misc.PoolPlayerSequencer;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.PlayerPool;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.util.BeanMapper;
import au.com.jwatmuff.eventmanager.util.BeanMapperTableModel;
import au.com.jwatmuff.eventmanager.util.GUIUtils;
import au.com.jwatmuff.genericdb.distributed.DataEvent;
import au.com.jwatmuff.genericdb.transaction.TransactionListener;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public abstract class PlayerTableModel extends BeanMapperTableModel<PlayerPoolInfo> implements TransactionListener {
    private static final Logger log = Logger.getLogger(PlayerTableModel.class);

    private List<PlayerPoolInfo> players = new ArrayList<PlayerPoolInfo>();
    private TransactionalDatabase database;
    private TransactionNotifier notifier;

    public PlayerTableModel(TransactionalDatabase database, TransactionNotifier notifier) {
        super();
        this.database = database;
        this.notifier = notifier;
        notifier.addListener(this, Pool.class, PlayerPool.class, Player.class);
        setBeanMapper(new BeanMapper<PlayerPoolInfo> () {
            @Override
            public Map<String, Object> mapBean(PlayerPoolInfo bean) {
                Map<String,Object> map = new HashMap<String, Object>();
                if(bean != null) {
                    if(bean.getPlayer().getTeam().isEmpty()){
                        map.put("name", "P" + bean.getPlayerPool().getPlayerPosition() + ": "+ bean.getPlayer().getFirstName() + " " + bean.getPlayer().getLastName() + "   (" + bean.getPlayer().getWeight() + " kg )");
                    } else {
                        map.put("name", "P" + bean.getPlayerPool().getPlayerPosition() + ": "+ bean.getPlayer().getFirstName() + " " + bean.getPlayer().getLastName() + "   (" + bean.getPlayer().getWeight() + " kg - " + bean.getPlayer().getTeam() + ")");
                    }
                } else {
                    map.put("name", "BYE");
                }
                return map;
            }
        });
        addColumn("Player", "name");
    }

    public abstract Pool getPool();

    public void updateFromDatabase() {
        Pool pool = getPool();
        if(pool != null) {
            //Collection<Player> players = database.findAll(Player.class, PlayerDAO.FOR_POOL, pool.getID(), true);
            players = PoolPlayerSequencer.getPlayerSequence(database, pool.getID());
            setBeans(players);
        }
    }

    public void shuffle() {
        List<PlayerPoolInfo> unorderedPlayers = new ArrayList<PlayerPoolInfo>();
        Pool pool = getPool();
        if(pool == null) return;
        int poolID = pool.getID();

        int numPlayerPositions = players.size();

        for(PlayerPoolInfo player : players) {
            if(player != null)
                unorderedPlayers.add(player);
        }

        players.clear();

        Collections.shuffle(unorderedPlayers);

        // fill at least half the available position in the ordered players list
        while(players.size() < numPlayerPositions/2 && !unorderedPlayers.isEmpty()) {
            players.add(unorderedPlayers.remove(0));
        }
        // fill at other half the available position in the ordered players list
        while(players.size()+unorderedPlayers.size() < numPlayerPositions) {
            unorderedPlayers.add(null);
        }
        Collections.shuffle(unorderedPlayers);
        players.addAll(unorderedPlayers);

        savePlayerSequence(database, poolID, players);
    }

    public void moveUp(int index) {
        Pool pool = getPool();
        if(pool == null) return;
        int poolID = pool.getID();

        if((index > 0) && (index < players.size())) {
            Collections.swap(players, index, index - 1);
            savePlayerSequence(database, poolID, players);
        }
    }

    public void moveDown(int index) {
        Pool pool = getPool();
        if(pool == null) return;
        int poolID = pool.getID();

        if((index >= 0) && (index < players.size()-1)) {
            Collections.swap(players, index, index+1);
            savePlayerSequence(database, poolID, players);
        }
    }

    public void savePlayerSequence() {
        Pool pool = getPool();
        if(pool == null) return;
        int poolID = pool.getID();

        savePlayerSequence(database, poolID, players);
    }

    private static void savePlayerSequence(TransactionalDatabase database, int poolID, List<PlayerPoolInfo> players) {
        try {
            PoolPlayerSequencer.savePlayerSequence(database, poolID, players);
        } catch (DatabaseStateException e) {
            log.error("Exception while saving player sequence", e);
            GUIUtils.displayError(null, "Error while updating player order");
        }
    }

    @Override
    public void handleTransactionEvents(List<DataEvent> events, Collection<Class> dataClasses) {
       updateFromDatabase();
    }

    public void shutdown() {
        notifier.removeListener(this);
    }
}
