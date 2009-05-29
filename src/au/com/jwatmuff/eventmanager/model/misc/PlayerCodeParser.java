/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.misc;

import au.com.jwatmuff.eventmanager.db.FightDAO;
import au.com.jwatmuff.eventmanager.model.info.FightInfo;
import au.com.jwatmuff.eventmanager.model.info.PlayerPoolInfo;
import au.com.jwatmuff.eventmanager.model.vo.Fight;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.genericdb.Database;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class PlayerCodeParser {
    private static final Logger log = Logger.getLogger(PlayerCodeParser.class);

    private static Pattern codePattern = Pattern.compile("(P++|[LW]++)(\\d++)");
    
    public static enum PlayerType {
        NORMAL, BYE, UNDECIDED, ERROR
    }
    
    public static class FightPlayer {
        public PlayerType type;
        public Player player;
        
        @Override
        public String toString() {
            switch(type) {
                case NORMAL:
                    return player.getFirstName() + " " + player.getLastName();
                case BYE:
                    return "Bye";
                case UNDECIDED:
                    return "Undecided";
            }
            return "Error";
        }
    }



    private PlayerCodeParser() {}
    
    public static boolean isValidCode(String code) {
        return codePattern.matcher(code).matches();
    }
    
    public static String getPrefix(String code) {
        Matcher matcher = codePattern.matcher(code);
        if(matcher.matches())
            return matcher.group(1);
        else
            throw new IllegalArgumentException("Invalid player code: " + code);
    }
    
    public static int getNumber(String code) {
        Matcher matcher = codePattern.matcher(code);
        if(matcher.matches())
            return Integer.parseInt(matcher.group(2));
        else
            throw new IllegalArgumentException("Invalid player code: " + code);
    }
    
    public static FightPlayer parseCode(String code, List<FightInfo> fights, List<PlayerPoolInfo> players) {
        if(!isValidCode(code))
            throw new IllegalArgumentException("Code not formatted correctly: '" + code + "'");
        
        FightPlayer fp = new FightPlayer();
        
        String prefix = getPrefix(code);
        int number = getNumber(code);

        if(prefix.equals("P")) {
            if(players.size() < number) {
                fp.type = PlayerType.BYE;
                return fp;
            } else {
                fp.type = PlayerType.NORMAL;
                fp.player = players.get(number-1).getPlayer();
                return fp;
            }
        }

        if(number > fights.size()) {
            fp.type = PlayerType.ERROR;
            return fp;
        }
        FightInfo fight = fights.get(number-1);
        
        while(prefix.length() > 0) {
            if(!fight.resultKnown()) {
                fp.type = PlayerType.UNDECIDED;
                return fp;
            }
        
            if(prefix.equals("W"))
                return parseCode(fight.getWinningPlayerCode(), fights, players);

            if(prefix.equals("L"))
                return parseCode(fight.getLosingPlayerCode(), fights, players);

            switch(prefix.charAt(prefix.length()-1)) {
                case 'W':
                    code = fight.getWinningPlayerCode();
                    break;
                case 'L':
                    code = fight.getLosingPlayerCode();
                    break;
                default:
                    fp.type = PlayerType.ERROR;
                    return fp;
            }
            
            if(!isValidCode(code) || getPrefix(code).equals("P")) {
                fp.type = PlayerType.ERROR;
                return fp;
            }

            try {
                fight = fights.get(getNumber(code)-1);
            } catch(IndexOutOfBoundsException e) {
                fp.type = PlayerType.ERROR;
                return fp;
            }
                    
            prefix = prefix.substring(0, prefix.length()-1);
        }
        
        fp.type = PlayerType.ERROR;
        return fp;
    }

    /* Parser instance stuff */

    List<PlayerPoolInfo> players;
    List<FightInfo> fi;

    private PlayerCodeParser(Database database, int poolID) {
        players = PoolPlayerSequencer.getPlayerSequence(database, poolID);
        List<Fight> fights = new ArrayList<Fight>(database.findAll(Fight.class, FightDAO.FOR_POOL, poolID));

        fi = new ArrayList<FightInfo>();
        for(Fight f : fights)
            fi.add(FightInfo.getFightInfo(database, f));
    }

    public static PlayerCodeParser getInstance(Database database, int poolID) {
        PlayerCodeParser parser = new PlayerCodeParser(database, poolID);
        return parser;
    }

    public FightPlayer parseCode(String code) {
        return PlayerCodeParser.parseCode(code, fi, players);
    }

    public static FightPlayer parseCode(Database database, String code, int poolID) throws DatabaseStateException {
        return PlayerCodeParser.getInstance(database, poolID).parseCode(code);
    }
}

