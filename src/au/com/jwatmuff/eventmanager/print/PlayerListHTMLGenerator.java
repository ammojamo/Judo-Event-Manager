/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.print;

import au.com.jwatmuff.eventmanager.db.PlayerDAO;
import au.com.jwatmuff.eventmanager.model.vo.CompetitionInfo;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.genericdb.Database;
import java.util.List;
import org.apache.velocity.context.Context;

/**
 *
 * @author James
 */
public class PlayerListHTMLGenerator extends VelocityHTMLGenerator {
    private List<Player> players;
    private Database database;

    public PlayerListHTMLGenerator(Database database) {
        this.database = database;
    }

    public PlayerListHTMLGenerator(Database database, List<Player> players) {
        this(database);
        this.players = players;
    }

    @Override
    public void populateContext(Context c) {
        if(players == null)
            c.put("players", database.findAll(Player.class, PlayerDAO.ALL));
        else
            c.put("players", players);

        c.put("competitionName", database.get(CompetitionInfo.class, null).getName());
    }

    @Override
    public String getTemplateName() {
        return "players.html";
    }
}
