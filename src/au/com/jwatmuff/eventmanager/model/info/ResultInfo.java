/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.info;

import au.com.jwatmuff.eventmanager.db.SessionFightDAO;
import au.com.jwatmuff.eventmanager.model.misc.DatabaseStateException;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser.FightPlayer;
import au.com.jwatmuff.eventmanager.model.misc.SessionFightSequencer;
import au.com.jwatmuff.eventmanager.model.misc.SessionFightSequencer.FightMatInfo;
import au.com.jwatmuff.eventmanager.model.vo.Fight;
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
    private FightPlayer[] fightPlayer = new FightPlayer[2];
    private String matName;
    private int matFightNumber;

    public ResultInfo(TransactionalDatabase database, int resultID) throws DatabaseStateException {
        result = database.get(Result.class, resultID);
        if(result == null) throw new DatabaseStateException();

        fight = database.get(Fight.class, result.getFightID());
        if(fight == null) throw new DatabaseStateException();

        for(int i=0; i<2; i++) {
            fightPlayer[i] = PlayerCodeParser.parseCode(database, fight.getPlayerCodes()[i], fight.getPoolID());
            playerName[i] = fightPlayer[i].player.getFirstName() + " " + fightPlayer[i].player.getLastName();
        }
        
        SessionFight sf = database.find(SessionFight.class, SessionFightDAO.FOR_FIGHT, fight.getID());
        if(sf == null) throw new DatabaseStateException();

        FightMatInfo info = SessionFightSequencer.getFightMatInfo(database, sf);
        matFightNumber = info.fightNumber;
        matName = info.matName;
        
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

    public FightPlayer[] getPlayer() {
        return fightPlayer;
    }

    public void setPlayer(FightPlayer[] player) {
        this.fightPlayer = player;
    }
}
