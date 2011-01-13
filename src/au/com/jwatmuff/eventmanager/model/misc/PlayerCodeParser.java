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
import au.com.jwatmuff.eventmanager.model.vo.Pool;
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

    // matches all possible player codes
    private static Pattern codePattern = Pattern.compile("(P|[LW]+|RAT?|RBT?)([0-9]+)(?:\\[([0-9]+(?:,[0-9]+)*)\\])?");
    
    public static enum PlayerType {
        NORMAL, BYE, UNDECIDED, ERROR
    }
    
    public static class FightPlayer {
        public PlayerType type;
        public Player player;
        public String code;
        public Pool division;
        
        @Override
        public String toString() {
            switch(type) {
                case NORMAL:
                    return player.getFirstName() + " " + player.getLastName();
                case BYE:
                    return "Bye";
                case UNDECIDED:
                    return code + " : " + division.getDescription();
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

    public static int[] getParameters(String code) {
        Matcher matcher = codePattern.matcher(code);
        if(matcher.matches()) {
            if(matcher.groupCount() >= 3) {
                String[] paramsStr = matcher.group(3).split(",");
                int[] params = new int[paramsStr.length];
                for(int i = 0; i < paramsStr.length; i++) {
                    params[i] = Integer.valueOf(paramsStr[i]);
                }
                return params;
            } else {
                return new int[0];
            }
        } else {
            throw new IllegalArgumentException("Invalid player code: " + code);
        }
    }
    
    public static FightPlayer parseCode(String code, List<FightInfo> fights, List<PlayerPoolInfo> players) {
        if(players == null || players.isEmpty())
            throw new IllegalArgumentException("Must supply a non-empty list of PlayerPoolInfos");

        // Use prefix to work out which parsing method to call
        String prefix = getPrefix(code);
        if(prefix.matches("P|[LW]+"))
            return parsePLWCode(code, fights, players);
        else if(prefix.matches("RAT?|RBT?"))
            return parseRRCode(code, fights, players);
        else
            throw new IllegalArgumentException("Code not formatted correctly: '" + code + "'");
    }

    private static FightPlayer parseRRCode(String code, List<FightInfo> fights, List<PlayerPoolInfo> players) {
                                           // e.g. RBT2[3,4,5]
        String prefix = getPrefix(code);   //      RBT
        int number = getNumber(code);      //      2
        int[] params = getParameters(code);//      3,4,5


        throw new RuntimeException("I'm not implemented yet!");
    }

    private static FightPlayer parsePLWCode(String code, List<FightInfo> fights, List<PlayerPoolInfo> players) {
        FightPlayer fp = new FightPlayer();

        fp.code = code;
        for(PlayerPoolInfo player : players) {
            if(player != null) {
                fp.division = player.getPool();
                break;
            }
        }
        
        String prefix = getPrefix(code);
        int number = getNumber(code);

        if(prefix.equals("P")) {
            if(players.size() < number) {
                fp.type = PlayerType.BYE;
                return fp;
            } else if(players.get(number-1) == null) {
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
            isABye:
            if(!fight.resultKnown()) {
                // This handles fights with a bye player, and always returns the
                // first bye player as the loser and the second bye or player as the winner.
                // This works through all levels.

                String[] codes = fight.getAllPlayerCode();
                FightPlayer[] fightPlayers = new FightPlayer[] {
                    parsePLWCode(codes[0], fights, players),
                    parsePLWCode(codes[1], fights, players)
                };
                for(int i = 0; i < 2; i++) {
                    int j = 1 - i; // other player
                    if(fightPlayers[i].type == PlayerType.BYE) {
                        if(prefix.equals("W")) return fightPlayers[j];
                        if(prefix.equals("L")) return fightPlayers[i];

                        switch(prefix.charAt(prefix.length()-1)) {
                            case 'W':
                                code = codes[j];
                                break isABye;
                            case 'L':
                                code = codes[i];
                                break isABye;
                            default:
                                fp.type = PlayerType.ERROR;
                                return fp;
                        }
                    }
                }
                fp.type = PlayerType.UNDECIDED;
                return fp;
            } else {
                if(prefix.equals("W"))
                    return parsePLWCode(fight.getWinningPlayerCode(), fights, players);

                if(prefix.equals("L"))
                    return parsePLWCode(fight.getLosingPlayerCode(), fights, players);

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
