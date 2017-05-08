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

import au.com.jwatmuff.eventmanager.model.info.SessionFightInfo;
import au.com.jwatmuff.eventmanager.model.misc.DatabaseStateException;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser.FightPlayer;
import au.com.jwatmuff.eventmanager.model.misc.SessionFightSequencer;
import au.com.jwatmuff.eventmanager.model.misc.SessionLinker;
import au.com.jwatmuff.eventmanager.model.vo.CompetitionInfo;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.model.vo.Session;
import au.com.jwatmuff.genericdb.Database;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.velocity.context.Context;

/**
 *
 * @author James
 */
public class FightOrderHTMLGenerator extends VelocityHTMLGenerator {
    private Database database;
    Session mat;
    List<Session> selectedSessions;

    public FightOrderHTMLGenerator(Database database, int matSessionID) {
        this.database = database;
        this.mat = database.get(Session.class, matSessionID);
        this.selectedSessions = SessionLinker.getMatSessions(database, mat);
    }

    public FightOrderHTMLGenerator(Database database, Session mat, List<Session> sessions) {
        this.database = database;
        this.mat = mat;
        this.selectedSessions = sessions;
    }

    @Override
    public void populateContext(Context c) {
        List<Session> sessions = new ArrayList<Session>();
        List<Integer> fightNumbers = new ArrayList<Integer>();
        Map<Integer, List<Map<String, String>>> fights = new HashMap<Integer, List<Map<String, String>>>();

        assert(mat.getType() == Session.SessionType.MAT);

        c.put("mat", mat.getMat());

        int fightNumber;

        for(Session session : selectedSessions) {
            sessions.add(session);

            fightNumber = SessionFightSequencer.getSessionFirstFightMatInfo(database, session.getID()).fightNumber;

            List<Map<String,String>> sessionFights = new ArrayList<Map<String,String>>();
            for(SessionFightInfo sfi : SessionFightSequencer.getFightSequence(database, session.getID())) {
                try {
                    Map<String, String> fight = new HashMap<String, String>();
                    FightPlayer fightPlayer1 = PlayerCodeParser.parseCode(database, sfi.getFight().getPlayerCodes()[0], sfi.getFight().getPoolID());
                    if(fightPlayer1.type == PlayerCodeParser.PlayerType.NORMAL){
                        fight.put("player1", fightPlayer1.toString() + " - " + fightPlayer1.player.getTeam().toString());
                    }else{
                        fight.put("player1", fightPlayer1.toString());
                    }
                    FightPlayer fightPlayer2 = PlayerCodeParser.parseCode(database, sfi.getFight().getPlayerCodes()[1], sfi.getFight().getPoolID());
                    if(fightPlayer2.type == PlayerCodeParser.PlayerType.NORMAL){
                        fight.put("player2", fightPlayer2.toString() + " - " + fightPlayer2.player.getTeam().toString());
                    }else{
                        fight.put("player2", fightPlayer2.toString());
                    }
                    fight.put("pool", database.get(Pool.class, sfi.getFight().getPoolID()).getDescription());
                    fight.put("poolNumber", "Fight " + sfi.getFight().getPosition());
                    sessionFights.add(fight);
                    fightNumbers.add(fightNumber++);
                } catch (DatabaseStateException e) { }
            }
            fights.put(session.getID(), sessionFights);
        }
        c.put("sessions", sessions);
        c.put("fights", fights);
        c.put("competitionName", database.get(CompetitionInfo.class, null).getName());
        c.put("fightNumbers", fightNumbers);
    }

    @Override
    public String getTemplateName() {
        return "fightorder.html";
    }
}
