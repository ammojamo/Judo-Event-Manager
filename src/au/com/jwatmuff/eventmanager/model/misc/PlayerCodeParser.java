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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;	
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
    static String matcherPlayer = "P";
    static String matcherWinnerLooser = "[LW]+";
    static String matcherRoundRobin = "RAP?|RAT?|RBT?|RBP?";
    static String matcherBestOfThree = "TAP?|TAT?";
    private static Pattern codePattern = Pattern.compile("(P|[LW]+|RAP?|RAT|RBP?|RBT?|TAP?|TAT?)([0-9]+)(?:-([0-9]+(?:-[0-9]+)*))?");
    
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
    
    public static enum CodeType {
        PLAYER, WINNERLOOSER, ROUNDROBIN, BESTOFTHREE, ERROR
    }

    public static class CodeInfo {
        public String code;
        public CodeType type;
        public String prefix;
        public int number;
        public int[] params;
    }
    
    public static class PlayerRRScore {
        int PlayerID;
        int PlayerPos2;
        int Wins;
        int Points;
        int Place;
        }

    private static Comparator<PlayerRRScore> PLAYERS_SCORE_COMPARATOR_RR = new Comparator<PlayerRRScore>(){
            @Override
            public int compare(PlayerRRScore p0, PlayerRRScore p1) {
                return (p1.Wins - p0.Wins != 0) ? p1.Wins - p0.Wins : p1.Points - p0.Points;
            }
        };

    private static Comparator<PlayerRRScore> PLAYERS_POS_COMPARATOR_RR = new Comparator<PlayerRRScore>(){
            @Override
            public int compare(PlayerRRScore p0, PlayerRRScore p1) {
                return p1.PlayerPos2 - p0.PlayerPos2;
            }
        };

    private static Comparator<PlayerRRScore> PLAYERS_SCORE_COMPARATOR_BT = new Comparator<PlayerRRScore>(){
            @Override
            public int compare(PlayerRRScore p0, PlayerRRScore p1) {
                return (p1.Wins - p0.Wins != 0) ? p1.Wins - p0.Wins : p1.Points - p0.Points;
            }
        };

    private static Comparator<PlayerRRScore> PLAYERS_POS_COMPARATOR_BT = new Comparator<PlayerRRScore>(){
            @Override
            public int compare(PlayerRRScore p0, PlayerRRScore p1) {
                return 0;
            }
        };

        
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
                String[] paramsStr = matcher.group(3).split("-");
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

    public static Player getPlayer(int playerID, List<PlayerPoolInfo> playerInfoList) {
        for(PlayerPoolInfo playerPoolInfo: playerInfoList ){
            if(playerPoolInfo != null && playerPoolInfo.getPlayer().getID() == playerID){
                return playerPoolInfo.getPlayer();
            }
        }
        return null;
    }

    public static CodeInfo getCodeInfo(String code) {
        CodeInfo codeInfo = new CodeInfo();
        codeInfo.code = code;
        if(!isValidCode(code)){
            codeInfo.type = CodeType.ERROR;
            return codeInfo;
        }
                                                // e.g. RBT2-3-4-5
        codeInfo.prefix = getPrefix(code);      //      RBT
        codeInfo.number = getNumber(code);      //      2

        if(codeInfo.prefix.matches(matcherPlayer))
            codeInfo.type = CodeType.PLAYER;
        else if(codeInfo.prefix.matches(matcherWinnerLooser))
            codeInfo.type = CodeType.WINNERLOOSER;
        else if(codeInfo.prefix.matches(matcherRoundRobin)){
            codeInfo.type = CodeType.ROUNDROBIN;
            codeInfo.params = getParameters(code);  //      3,4,5
        } else if (codeInfo.prefix.matches(matcherBestOfThree)) {
            codeInfo.type = CodeType.ROUNDROBIN;
            codeInfo.params = getParameters(code);  //      3,4,5
        } else
            throw new IllegalArgumentException("Code not formatted correctly: '" + code + "'");
        return codeInfo;
    }

    public static String[] getORCodes(String code) {
        String[] codes = code.split("\\|");
        return codes;
    }

    public static FightPlayer parseCode(String code, List<FightInfo> fightInfoList, List<PlayerPoolInfo> playerInfoList) {
        if(playerInfoList == null || playerInfoList.isEmpty())
            throw new IllegalArgumentException("Must supply a non-empty list of PlayerPoolInfos");

        String[] codes = getORCodes(code);
        
        if(codes.length == 1){
            // Use prefix to work out which parsing method to call
            String prefix = getPrefix(code);
            if(prefix.matches(matcherPlayer))
                return parsePLWCode(code, fightInfoList, playerInfoList);
            else if(prefix.matches(matcherWinnerLooser))
                return parsePLWCode(code, fightInfoList, playerInfoList);
            else if(prefix.matches(matcherRoundRobin))
                return parseRRCode(code, fightInfoList, playerInfoList);
            else if(prefix.matches(matcherBestOfThree))
                return parseRRCode(code, fightInfoList, playerInfoList);
            else
                throw new IllegalArgumentException("Code not formatted correctly: '" + code + "'");
        } else if(getCodeInfo(codes[0]).type == CodeType.ROUNDROBIN || getCodeInfo(codes[0]).type == CodeType.BESTOFTHREE){

            String prefix = getPrefix(codes[0]);   //      RBT
            int number = getNumber(codes[0]);      //      2
            int[] params = getParameters(codes[0]);//      3,4,5

            FightPlayer fightPlayer = parseCode(codes[0], fightInfoList, playerInfoList);
            if(fightPlayer.type == PlayerType.UNDECIDED) {
                int codePnt = 1;
                for(int i=1; i < number; i++) {
                    String newCode = prefix + i;
                    for(int j = 0; j < params.length; j++)
                        newCode = newCode + "-" + params[j];
                    if(parseCode(newCode, fightInfoList, playerInfoList).type == PlayerType.UNDECIDED)
                        codePnt = codePnt + 1;
                }
                fightPlayer = parseCode(codes[codePnt], fightInfoList, playerInfoList);
                fightPlayer.code = codes[0];
            }
            fightPlayer.code = codes[0];
            return fightPlayer;
        } else {
            throw new IllegalArgumentException("Invalid player code: " + code);
        }
    }

    private static FightPlayer parseRRCode(String code, List<FightInfo> fightInfoList, List<PlayerPoolInfo> playerInfoList) {
        FightPlayer fightPlayer = new FightPlayer();
        CodeType codeType = getCodeInfo(code).type;

        List<FightInfo> roundRobinFights = new ArrayList<FightInfo>();

        fightPlayer.code = code;
        for(PlayerPoolInfo playerPoolInfo : playerInfoList) {
            if(playerPoolInfo != null) {
                fightPlayer.division = playerPoolInfo.getPool();
                break;
            }
        }
                                           // e.g. RBT2-3-4-5
        String prefix = getPrefix(code);   //      RBT
        int number = getNumber(code);      //      2
        int[] params = getParameters(code);//      3,4,5

        for(int roundRobinFight : params) {
            if(roundRobinFight > fightInfoList.size()) {
                fightPlayer.type = PlayerType.ERROR;
                return fightPlayer;
            }
            FightInfo fight = fightInfoList.get(roundRobinFight-1);
            if(!fight.resultKnown()) {
                fightPlayer.type = PlayerType.UNDECIDED;
                return fightPlayer;
            }
            roundRobinFights.add(fight);
        }

        if(!roundRobinFights.isEmpty()){
            List<PlayerRRScore> PlayerRRScore = RoundRobinResults(codeType, roundRobinFights, playerInfoList);
            if(PlayerRRScore.size()<number){
                fightPlayer.type = PlayerType.ERROR;
                return fightPlayer;
            }

            boolean isTie = false;
            switch(prefix.charAt(2)) {

                case 'T':
// Offset number by start of Tie
                    for(int tieStart = 0 ; tieStart < PlayerRRScore.size()-1 ; tieStart++){
                        if(PlayerRRScore.get(tieStart).Place == PlayerRRScore.get(tieStart+1).Place ){
                            number = number + tieStart;
                            break;
                        }
                    }

// Check for Tie with another player
                    isTie = false;
                    if(PlayerRRScore.size() >= number){
                        if(PlayerRRScore.size() > number){
                            if(PlayerRRScore.get(number-1).Place == PlayerRRScore.get(number).Place ){
                                isTie = true;
                            }
                        }
                        if (number > 1){
                            if(PlayerRRScore.get(number-1).Place == PlayerRRScore.get(number-2).Place ){
                                isTie = true;
                            }
                        }
                    }
                    if(isTie){
                        fightPlayer.type = PlayerType.NORMAL;
                        fightPlayer.player = getPlayer(PlayerRRScore.get(number-1).PlayerID, playerInfoList);
                        return fightPlayer;
                    } else {
                        fightPlayer.type = PlayerType.BYE;
                        return fightPlayer;
                    }

                case 'P':
// Check for Tie with another player
                    isTie = false;
                    if(PlayerRRScore.size() > number){
                        if(PlayerRRScore.get(number-1).Place == PlayerRRScore.get(number).Place ){
                            isTie = true;
                        }
                    }
                    if (number>1){
                        if(PlayerRRScore.get(number-1).Place == PlayerRRScore.get(number-2).Place ){
                            isTie = true;
                        }
                    }
                    if(isTie){
                        fightPlayer.type = PlayerType.UNDECIDED;
                        return fightPlayer;
                    } else {
                        fightPlayer.type = PlayerType.NORMAL;
                        fightPlayer.player = getPlayer(PlayerRRScore.get(number-1).PlayerID, playerInfoList);
                        return fightPlayer;
                    }

                default:
                    fightPlayer.type = PlayerType.ERROR;
                    return fightPlayer;
            }
        }
//        throw new RuntimeException("I'm not implemented yet!");
        fightPlayer.type = PlayerType.ERROR;
        return fightPlayer;
    }

    private static List<PlayerRRScore> RoundRobinResults(CodeType codeType, List<FightInfo> fightInfoList, List<PlayerPoolInfo> playerInfoList) {

//Add all piList in fights to map
        Map<Integer,PlayerRRScore> playerRRScoresMap = new HashMap<Integer,PlayerRRScore>();
        for(FightInfo fightInfo : fightInfoList) {
            for(int playerID : fightInfo.getAllPlayerID()){
                if (!playerRRScoresMap.containsKey(playerID)){
                    PlayerRRScore playerRRScore = new PlayerRRScore();
                    playerRRScore.PlayerID = playerID;
                    for(PlayerPoolInfo playerInfo : playerInfoList){
                        if(playerInfo.getPlayerPool().getPlayerID() == playerID){
                            playerRRScore.PlayerPos2 = playerInfo.getPlayerPool().getPlayerPosition2();
                            break;
                        }
                    }
                    playerRRScore.Wins = 0;
                    playerRRScore.Points = 0;
                    playerRRScore.Place = 0;
                    playerRRScoresMap.put(playerID,playerRRScore);
                }
            }
        }

//Calculate accumulated wins and points
        for(FightInfo fightInfo : fightInfoList) {
            int winningPlayerSimpleScore = fightInfo.getWinningPlayerSimpleScore();
            PlayerRRScore playerRRScore = playerRRScoresMap.get(fightInfo.getWinningPlayerID());
            playerRRScore.Wins = playerRRScore.Wins+1;
            playerRRScore.Points = playerRRScore.Points + winningPlayerSimpleScore;
            playerRRScoresMap.put(fightInfo.getWinningPlayerID(), playerRRScore);
        }
        
//Convert mat to a list for sorting
        List<PlayerRRScore> playerRRScoresList = new ArrayList<PlayerRRScore>();
        for(Integer playerID : playerRRScoresMap.keySet()) {
            playerRRScoresList.add(playerRRScoresMap.get(playerID));
        }

        Comparator<PlayerRRScore> PLAYERS_SCORE_COMPARATOR = PLAYERS_SCORE_COMPARATOR_RR;
        Comparator<PlayerRRScore> PLAYERS_POS_COMPARATOR = PLAYERS_POS_COMPARATOR_RR;
        if(codeType == CodeType.ROUNDROBIN){
            PLAYERS_SCORE_COMPARATOR = PLAYERS_SCORE_COMPARATOR_BT;
            PLAYERS_POS_COMPARATOR = PLAYERS_POS_COMPARATOR_BT;
        }
        
//Sort List
        Collections.sort(playerRRScoresList, PLAYERS_SCORE_COMPARATOR);
        
        
//find equals
        List<PlayerRRScore> playerRRTieList = new ArrayList<PlayerRRScore>();
        int i = 0;
        while(i < playerRRScoresList.size()) {
            int j = i;
            playerRRTieList.clear();
            while(j < playerRRScoresList.size()-1 && PLAYERS_SCORE_COMPARATOR.compare(playerRRScoresList.get(j), playerRRScoresList.get(j+1))==0){
                j++;
            }
            if(j > i){
                for(int k = i; k <= j; k++)
                    playerRRTieList.add(playerRRScoresList.get(k));
            }
// Are ZERO players tied?
            if(playerRRTieList.isEmpty()){
                playerRRScoresList.get(i).Place = i;

// Are ALL players tied?
            } else if(playerRRScoresList.size()==playerRRTieList.size()){   //if all equal return place = 0

                for(int k = 0; k < playerRRScoresList.size(); k++){
                    playerRRScoresList.get(k).Place = 0;
                }
                Collections.sort(playerRRScoresList, PLAYERS_POS_COMPARATOR);
                return playerRRScoresList;

// Are SOME players tied?
            }else{
                List<FightInfo> newFights = new ArrayList<FightInfo>();
                int matchedIDs;
                for(FightInfo fightInfo : fightInfoList){
                    matchedIDs = 0;
                    for(int k = 0; k < playerRRTieList.size(); k++){
                        if(playerRRTieList.get(k).PlayerID==fightInfo.getLosingPlayerID() || playerRRTieList.get(k).PlayerID==fightInfo.getWinningPlayerID()){
                            matchedIDs++;
                        }
                    }
                    if(matchedIDs == 2)
                        newFights.add(fightInfo);
                }
                playerRRTieList = RoundRobinResults( codeType, newFights,  playerInfoList);
                for(int k = 0; k < playerRRTieList.size(); k++){
                    playerRRTieList.get(k).Place = playerRRTieList.get(k).Place+i;
                    playerRRScoresList.set(i+k, playerRRTieList.get(k));
                }
                i = i + playerRRTieList.size()-1;
            }

            i++;
        }
        
        return playerRRScoresList;
    }

    private static FightPlayer parsePLWCode(String code, List<FightInfo> fightInfoList, List<PlayerPoolInfo> playerInfoList) {
        FightPlayer fightPlayer = new FightPlayer();

        fightPlayer.code = code;
        for(PlayerPoolInfo player : playerInfoList) {
            if(player != null) {
                fightPlayer.division = player.getPool();
                break;
            }
        }
        
        String prefix = getPrefix(code);
        int number = getNumber(code);

        if(prefix.equals("P")) {
            if(playerInfoList.size() < number) {
                fightPlayer.type = PlayerType.BYE;
                return fightPlayer;
            } else if(playerInfoList.get(number-1) == null) {
                fightPlayer.type = PlayerType.BYE;
                return fightPlayer;
            } else {
                fightPlayer.type = PlayerType.NORMAL;
                fightPlayer.player = playerInfoList.get(number-1).getPlayer();
                return fightPlayer;
            }
        }

        if(number > fightInfoList.size()) {
            fightPlayer.type = PlayerType.ERROR;
            return fightPlayer;
        }

        FightInfo fight = fightInfoList.get(number-1);
        while(prefix.length() > 0) {
            isABye:
            if(!fight.resultKnown()) {
                // This handles fights with a bye player, and always returns the
                // first bye player as the loser and the second bye or player as the winner.
                // This works through all levels.

                String[] codes = fight.getAllPlayerCode();
                FightPlayer[] fightPlayers = new FightPlayer[] {
                    parseCode(codes[0], fightInfoList, playerInfoList),
                    parseCode(codes[1], fightInfoList, playerInfoList)
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
                                fightPlayer.type = PlayerType.ERROR;
                                return fightPlayer;
                        }
                    }
                }
                fightPlayer.type = PlayerType.UNDECIDED;
                return fightPlayer;

            } else {

                if(prefix.equals("W")){
                    fightPlayer.type = PlayerType.NORMAL;
                    fightPlayer.player = getPlayer( fight.getWinningPlayerID(), playerInfoList);
                    if(fightPlayer.player == null)
                        fightPlayer.type = PlayerType.ERROR;
                    return fightPlayer;
                }
                if(prefix.equals("L")){
                    fightPlayer.type = PlayerType.NORMAL;
                    fightPlayer.player = getPlayer( fight.getLosingPlayerID(), playerInfoList);
                    if(fightPlayer.player == null)
                        fightPlayer.type = PlayerType.ERROR;
                    return fightPlayer;
                }

                switch(prefix.charAt(prefix.length()-1)) {
                    case 'W':
                        code = fight.getWinningPlayerCode();
                        break;
                    case 'L':
                        code = fight.getLosingPlayerCode();
                        break;
                    default:
                        fightPlayer.type = PlayerType.ERROR;
                        return fightPlayer;
                }
            }
            
            if(!isValidCode(code) || getPrefix(code).equals("P")) {
                fightPlayer.type = PlayerType.ERROR;
                return fightPlayer;
            }

            try {
                fight = fightInfoList.get(getNumber(code)-1);
            } catch(IndexOutOfBoundsException e) {
                fightPlayer.type = PlayerType.ERROR;
                return fightPlayer;
            }
                    
            prefix = prefix.substring(0, prefix.length()-1);
        }
        
        fightPlayer.type = PlayerType.ERROR;
        return fightPlayer;
    }

    /* Parser instance stuff */

    List<PlayerPoolInfo> piList;
    List<FightInfo> fiList;

    private PlayerCodeParser(Database database, int poolID) {
        piList = PoolPlayerSequencer.getPlayerSequence(database, poolID);
        List<Fight> fights = new ArrayList<Fight>(database.findAll(Fight.class, FightDAO.FOR_POOL, poolID));

        fiList = new ArrayList<FightInfo>();
        for(Fight fight : fights)
            fiList.add(FightInfo.getFightInfo(database, fight));
    }

    public static PlayerCodeParser getInstance(Database database, int poolID) {
        PlayerCodeParser parser = new PlayerCodeParser(database, poolID);
        return parser;
    }

    public FightPlayer parseCode(String code) {
        return PlayerCodeParser.parseCode(code, fiList, piList);
    }

    public static FightPlayer parseCode(Database database, String code, int poolID) throws DatabaseStateException {
        return PlayerCodeParser.getInstance(database, poolID).parseCode(code);
    }
}
