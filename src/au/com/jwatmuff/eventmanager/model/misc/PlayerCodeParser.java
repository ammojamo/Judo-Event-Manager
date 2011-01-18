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
import au.com.jwatmuff.eventmanager.model.vo.Pool.Place;
import au.com.jwatmuff.genericdb.Database;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
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
    private static Pattern codePattern = Pattern.compile("(P|[LW]+|RAP?|RAT?)([0-9]+)(?:-([0-9]+(?:-[0-9]+)*))?");
    
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
    
    public static class PlayerRRScore {
        int PlayerID;
        int PlayerPos2;
        int Wins;
        int Points;
        int Place;
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
    
    public static FightPlayer parseCode(String code, List<FightInfo> fightInfoList, List<PlayerPoolInfo> playerInfoList) {
        if(playerInfoList == null || playerInfoList.isEmpty())
            throw new IllegalArgumentException("Must supply a non-empty list of PlayerPoolInfos");

        String[] codes = code.split("\\|");
        if(codes.length == 1){
            // Use prefix to work out which parsing method to call
            String prefix = getPrefix(code);
            if(prefix.matches("P|[LW]+"))
                return parsePLWCode(code, fightInfoList, playerInfoList);
            else if(prefix.matches("RAP?|RAT?|RCT?|RDT?"))
                return parseRRCode(code, fightInfoList, playerInfoList);
            else
                throw new IllegalArgumentException("Code not formatted correctly: '" + code + "'");
        } else{
            throw new IllegalArgumentException("Invalid player code: " + code);
        }
    }

    private static FightPlayer parseRRCode(String code, List<FightInfo> fightInfoList, List<PlayerPoolInfo> playerInfoList) {
        FightPlayer fp = new FightPlayer();
        List<FightInfo> roundRobinFights = new ArrayList<FightInfo>();

        fp.code = code;
        for(PlayerPoolInfo playerPoolInfo : playerInfoList) {
            if(playerPoolInfo != null) {
                fp.division = playerPoolInfo.getPool();
                break;
            }
        }
                                           // e.g. RBT2-3-4-5
        String prefix = getPrefix(code);   //      RBT
        int number = getNumber(code);      //      2
        int[] params = getParameters(code);//      3,4,5

        for(int roundRobinFight : params) {
            if(roundRobinFight > fightInfoList.size()) {
                fp.type = PlayerType.ERROR;
                return fp;
            }
            FightInfo fight = fightInfoList.get(roundRobinFight-1);
            if(!fight.resultKnown()) {
                fp.type = PlayerType.UNDECIDED;
                return fp;
            }
            roundRobinFights.add(fight);
        }

        if(!roundRobinFights.isEmpty()){
            List<PlayerRRScore> PlayerRRScore = RoundRobinResults(roundRobinFights, playerInfoList);
            if(PlayerRRScore.size()<number){
                fp.type = PlayerType.ERROR;
                return fp;
            }

            boolean isTie = false;
            if(PlayerRRScore.size()>number){
                if(PlayerRRScore.get(number-1).Place == PlayerRRScore.get(number).Place ){
                    isTie = true;
                }
            }else if (number>1){
                if(PlayerRRScore.get(number-1).Place == PlayerRRScore.get(number-2).Place ){
                    isTie = true;
                }
            }
            if(isTie){
                switch(prefix.charAt(2)) {
                    case 'T':
                        for(PlayerPoolInfo playerPoolInfo : playerInfoList){
                            if(playerPoolInfo.getPlayer().getID() == PlayerRRScore.get(number-1).PlayerID){
                                fp.player = playerPoolInfo.getPlayer();
                                fp.type = PlayerType.NORMAL;
                                return fp;
                            }
                        }
                    case 'P':
                        fp.type = PlayerType.UNDECIDED;
                        return fp;
                    default:
                        fp.type = PlayerType.ERROR;
                        return fp;
                }
            } else {
                switch(prefix.charAt(2)) {
                    case 'T':
                        fp.type = PlayerType.BYE;
                        return fp;
                    case 'P':
                        for(PlayerPoolInfo playerPoolInfo : playerInfoList){
                            if(playerPoolInfo.getPlayer().getID() == PlayerRRScore.get(number-1).PlayerID){
                                fp.player = playerPoolInfo.getPlayer();
                                fp.type = PlayerType.NORMAL;
                                return fp;
                            }
                        }
                    default:
                        fp.type = PlayerType.ERROR;
                        return fp;
                }
            }
        }
//        throw new RuntimeException("I'm not implemented yet!");
        fp.type = PlayerType.ERROR;
        System.out.println("**fp.type = PlayerType.ERROR;**************************************************************************************");
        return fp;
    }

    private static List<PlayerRRScore> RoundRobinResults(List<FightInfo> fightInfoList, List<PlayerPoolInfo> playerInfoList) {

//Add all piList in fights to map
        Map<Integer,PlayerRRScore> playerRRScoresMap = new HashMap<Integer,PlayerRRScore>();
        for(FightInfo fightInfo : fightInfoList) {
            for(int playerID : fightInfo.getAllPlayerID()){
                if (!playerRRScoresMap.containsKey(playerID)){
                    PlayerRRScore pRRS = new PlayerRRScore();
                    pRRS.PlayerID = playerID;
                    for(PlayerPoolInfo playerInfo : playerInfoList){
                        if(playerInfo.getPlayerPool().getPlayerID() == playerID){
                            pRRS.PlayerPos2 = playerInfo.getPlayerPool().getPlayerPosition2();
                            break;
                        }
                    }
                    pRRS.Wins = 0;
                    pRRS.Points = 0;
                    pRRS.Place = 0;
                    playerRRScoresMap.put(playerID,pRRS);
                }
            }
        }

//Calculate accumulated wins and points
        for(FightInfo fightInfo : fightInfoList) {
            int winningPlayerSimpleScore = fightInfo.getWinningPlayerSimpleScore();
            PlayerRRScore pRRS = playerRRScoresMap.get(fightInfo.getWinningPlayerID());
            pRRS.Wins = pRRS.Wins+1;
            pRRS.Points = pRRS.Points + winningPlayerSimpleScore;
            playerRRScoresMap.put(fightInfo.getWinningPlayerID(), pRRS);
        }
        
//Convert mat to a list for sorting
        List<PlayerRRScore> playerRRScoresList = new ArrayList<PlayerRRScore>();
        for(Integer playerID : playerRRScoresMap.keySet()) {
            playerRRScoresList.add(playerRRScoresMap.get(playerID));
        }
        
//Sort List
        Comparator<PlayerRRScore> PLAYERS_SCORE_COMPARATOR = new Comparator<PlayerRRScore>(){
            @Override
            public int compare(PlayerRRScore p0, PlayerRRScore p1) {
                return (p1.Wins - p0.Wins != 0) ? p1.Wins - p0.Wins : p1.Points - p0.Points;
            }
        };
        Comparator<PlayerRRScore> PLAYERS_POS_COMPARATOR = new Comparator<PlayerRRScore>(){
            @Override
            public int compare(PlayerRRScore p0, PlayerRRScore p1) {
                return p1.PlayerPos2 - p0.PlayerPos2;
            }
        };
        
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
            if(playerRRTieList.isEmpty()){
                playerRRScoresList.get(i).Place = i;
                
            } else if(playerRRScoresList.size()==playerRRTieList.size()){   //if all equal return place = 0

                for(int k = 0; k < playerRRScoresList.size(); k++){
                    playerRRScoresList.get(k).Place = 0;
                }
                Collections.sort(playerRRScoresList, PLAYERS_POS_COMPARATOR);

                return playerRRScoresList;
                
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
                playerRRTieList = RoundRobinResults( newFights,  playerInfoList);
                for(int k = 0; k <= playerRRTieList.size(); k++){
                    playerRRTieList.get(k).Place = playerRRTieList.get(k).Place+i;
                    playerRRScoresList.add(i+k, playerRRTieList.get(k));
                }
            }

            i++;
        }
//System.out.println("*****************************************************************************************");
//        for(int k = 0; k < playerRRScoresList.size(); k++){
//System.out.println(playerRRScoresList.get(k).PlayerID + " Place: " + playerRRScoresList.get(k).Place + " Wins: " + playerRRScoresList.get(k).Wins);
//        }
        
        return playerRRScoresList;
    }

    private static FightPlayer parsePLWCode(String code, List<FightInfo> fightInfoList, List<PlayerPoolInfo> playerInfoList) {
        FightPlayer fp = new FightPlayer();

        fp.code = code;
        for(PlayerPoolInfo player : playerInfoList) {
            if(player != null) {
                fp.division = player.getPool();
                break;
            }
        }
        
        String prefix = getPrefix(code);
        int number = getNumber(code);

        if(prefix.equals("P")) {
            if(playerInfoList.size() < number) {
                fp.type = PlayerType.BYE;
                return fp;
            } else if(playerInfoList.get(number-1) == null) {
                fp.type = PlayerType.BYE;
                return fp;
            } else {
                fp.type = PlayerType.NORMAL;
                fp.player = playerInfoList.get(number-1).getPlayer();
                return fp;
            }
        }

        if(number > fightInfoList.size()) {
        System.out.println("**if(number > fightInfoList.size()) {**************************************************************************************");
            fp.type = PlayerType.ERROR;
            return fp;
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
                                fp.type = PlayerType.ERROR;
                                return fp;
                        }
                    }
                }
                fp.type = PlayerType.UNDECIDED;
                return fp;
            } else {
                if(prefix.equals("W"))
                    return parseCode(fight.getWinningPlayerCode(), fightInfoList, playerInfoList);

                if(prefix.equals("L"))
                    return parseCode(fight.getLosingPlayerCode(), fightInfoList, playerInfoList);

                switch(prefix.charAt(prefix.length()-1)) {
                    case 'W':
                        code = fight.getWinningPlayerCode();
                        break;
                    case 'L':
                        code = fight.getLosingPlayerCode();
                        break;
                    default:
        System.out.println("**switch(prefix.charAt(prefix.length()-1)) {**************************************************************************************");
                        fp.type = PlayerType.ERROR;
                        return fp;
                }
            }
            
            if(!isValidCode(code) || getPrefix(code).equals("P")) {
                fp.type = PlayerType.ERROR;
                return fp;
            }

            try {
                fight = fightInfoList.get(getNumber(code)-1);
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

    List<PlayerPoolInfo> piList;
    List<FightInfo> fiList;

    private PlayerCodeParser(Database database, int poolID) {
        piList = PoolPlayerSequencer.getPlayerSequence(database, poolID);
//System.out.println("*********************");
//for(int k = 0; k < piList.size(); k++){
//    System.out.println(piList.get(k).getPlayerPool().getPlayerID() + " pp2 " + piList.get(k).getPlayerPool().getPlayerPosition2());
//}
        List<Fight> fights = new ArrayList<Fight>(database.findAll(Fight.class, FightDAO.FOR_POOL, poolID));

        fiList = new ArrayList<FightInfo>();
        for(Fight f : fights)
            fiList.add(FightInfo.getFightInfo(database, f));
    }

    public static PlayerCodeParser getInstance(Database database, int poolID) {
        PlayerCodeParser parser = new PlayerCodeParser(database, poolID);
        return parser;
    }

    public FightPlayer parseCode(String code) {
        return PlayerCodeParser.parseCode(code, fiList, piList);
    }

    public Map<Place,FightPlayer> parsePlaces(List<Place> places) {
        List<FightPlayer> placeFightPlayer = new ArrayList<FightPlayer>();
        Map<Place,FightPlayer> placeAndFightPlayer = new HashMap<Place,FightPlayer>();
        for(Place place : places) {
            thisPlace:
            for(String placeCode : place.code.split("\\|")) {
                FightPlayer p = parseCode(placeCode);

                switch(p.type) {
                    case NORMAL:
                        placeAndFightPlayer.put(place, p);
                        break thisPlace;
                    case ERROR:
                        placeAndFightPlayer.put(place, p);
                        break thisPlace;
                    case UNDECIDED:
                        placeAndFightPlayer.put(place, p);
                        break;
                    default:
                        placeAndFightPlayer.put(place, p);
                        break thisPlace;
                }
            }
        }
        return placeAndFightPlayer;
    }

    public static FightPlayer parseCode(Database database, String code, int poolID) throws DatabaseStateException {
        return PlayerCodeParser.getInstance(database, poolID).parseCode(code);
    }
}
