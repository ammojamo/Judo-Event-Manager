package au.com.jwatmuff.eventmanager.gui.fightorder;

import au.com.jwatmuff.eventmanager.db.FightDAO;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser.FightPlayer;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser.PlayerType;
import au.com.jwatmuff.eventmanager.model.vo.Fight;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.util.BeanMapper;
import au.com.jwatmuff.eventmanager.util.BeanMapperTableModel;
import au.com.jwatmuff.genericdb.distributed.DataEvent;
import au.com.jwatmuff.genericdb.transaction.TransactionListener;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author James
 */
public abstract class FightTableModel extends BeanMapperTableModel<Fight> implements TransactionListener {
    private PlayerCodeParser playerCodeParser;
    private TransactionalDatabase database;
    private TransactionNotifier notifier;

    public FightTableModel(TransactionalDatabase database, TransactionNotifier notifier) {
        this.database = database;
        this.notifier = notifier;
        notifier.addListener(this, Fight.class);

        setBeanMapper(new BeanMapper<Fight>() {
            @Override
            public Map<String, Object> mapBean(Fight bean) {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("position", bean.getPosition());

                for(int i = 0; i < 2; i++) {
                    String code = bean.getPlayerCodes()[i];
                    FightPlayer fp = playerCodeParser.parseCode(code);
                    if(fp.type == PlayerType.NORMAL)
                      map.put("player" + (i+1), code + ": " + fp.toString());
                    else
                      map.put("player" + (i+1), code + ": " + fp.type);
                }

                return map;
            }
        });
        addColumn("Sequence #", "position");
        addColumn("Player 1", "player1");
        addColumn("Player 2", "player2");
    }

    public abstract Pool getPool();

    public void updateFromDatabase() {
        Pool pool = getPool();
        if(pool != null) {
            playerCodeParser = PlayerCodeParser.getInstance(database, pool.getID());
            Collection<Fight> fights = database.findAll(Fight.class, FightDAO.FOR_POOL, pool.getID());
            setBeans(fights);
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
