/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.info;

import au.com.jwatmuff.eventmanager.db.SessionFightDAO;
import au.com.jwatmuff.eventmanager.model.misc.DatabaseStateException;
import au.com.jwatmuff.eventmanager.model.misc.SessionFightSequencer;
import au.com.jwatmuff.eventmanager.model.misc.SessionFightSequencer.FightMatInfo;
import au.com.jwatmuff.eventmanager.model.vo.Fight;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.Result;
import au.com.jwatmuff.eventmanager.model.vo.SessionFight;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;

/**
 *
 * @author James
 */
public class ResultInfo {
    private Result result;
    private Fight fight;
    private String[] playerName = new String[2];
    private Player[] players = new Player[2];
    private String matName;
    private int matFightNumber;

    public ResultInfo(TransactionalDatabase database, int resultID) throws DatabaseStateException {
        result = database.get(Result.class, resultID);
        if(result == null) throw new DatabaseStateException();

        fight = database.get(Fight.class, result.getFightID());
        if(fight == null) throw new DatabaseStateException();

        for(int i=0; i<2; i++) {
            players[i] = database.get(Player.class, result.getPlayerIDs()[i]);
            playerName[i] = players[i].getFirstName() + " " + players[i].getLastName();
        }
        
        try {
            SessionFight sf = database.find(SessionFight.class, SessionFightDAO.FOR_FIGHT, fight.getID());

            FightMatInfo info = SessionFightSequencer.getFightMatInfo(database, sf);
            matFightNumber = info.fightNumber;
            matName = info.matName;
        } catch(Exception e) {
            matFightNumber = 0;
            matName = "(changed)";
        }
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public Fight getFight() {
        return fight;
    }

    public void setFight(Fight fight) {
        this.fight = fight;
    }

    public String[] getPlayerName() {
        return playerName;
    }

    public String getMatName() {
        return matName;
    }

    public void setMatName(String matName) {
        this.matName = matName;
    }

    public int getMatFightNumber() {
        return matFightNumber;
    }

    public void setMatFightNumber(int matFightNumber) {
        this.matFightNumber = matFightNumber;
    }

    public Player[] getPlayer() {
        return players;
    }
}
