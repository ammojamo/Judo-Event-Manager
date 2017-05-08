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
