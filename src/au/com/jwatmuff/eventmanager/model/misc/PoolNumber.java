/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.misc;


import org.apache.log4j.Logger; 
import java.util.ArrayList;
import java.util.List;

import java.util.Random;
import java.util.HashMap;
import java.util.Map;


/**
 *
 * @author Leonard Hall
 */
public class PoolNumber {
    
    private static final Logger log = Logger.getLogger(PoolNumber.class);

    public static class PoolNo {
        public int poolNumber;
        public int poolOrder;
    }

    public static class PoolNoPath {
        public List<Integer> Corners = new ArrayList<Integer>();
        public List<Integer> Directions = new ArrayList<Integer>();
    }

    
    /** Creates a new instance of PlayerPoolInfo */
    public static PoolNo PoolNumber(int poolNumber, int poolOrder) {
        PoolNo poolNo = new PoolNo();
        poolNo.poolNumber = poolNumber;
        poolNo.poolOrder = poolOrder;
        return poolNo;
    }

    public static int GetPoolNumber(PoolNo poolNo) {
        return poolNo.poolNumber;
    }

    public static PoolNoPath GetPoolNumberPath(PoolNo poolNo) {
        PoolNoPath poolNoPath = new PoolNoPath();
        int Order = poolNo.poolOrder;
        for(int i = 0; i<Order; i++){
            poolNoPath.Directions.add(0,GetLeading(poolNo));
            poolNoPath.Corners.add(0,poolNo.poolNumber);
            poolNo = ReduceOrder(poolNo);
        }
        return poolNoPath;
    }

    public static PoolNo FollowPath(List<Integer> path) {
        PoolNo poolNo = PoolNumber(0, 0);
        for(int i = 0; i<path.size(); i++){
            poolNo = IncreaseOrder(poolNo, path.get(i));
        }
        return poolNo;
    }

    public static int GetPoolOrder(PoolNo poolNo) {
        return poolNo.poolOrder;
    }

    public static int NumberToOrder(int number) {
        int order = 0;
        while (number > 0){
            number = number >>> 1;
            order++;
        }
        return order;
    }

    public static int MinimumOrder(int number) {
        int order = 0;
        number = number - 1;
        while (number > 0){
            number = number >>> 1;
            order++;
        }
        order++;
        return order;
    }

    public static int OrderToNumber(int order) {
        int number = 1;
        for(int i = 0; i < order-1; i++)
            number = number << 1;
        return number;
    }

    private static int GetLeading(PoolNo poolNo) {
        int leading = poolNo.poolNumber >> (poolNo.poolOrder - 1);
        int mask = 1;
        leading = leading & mask;
        return leading;
    }

    private static PoolNo AddLeading(PoolNo poolNo) {
        poolNo.poolOrder++;
        return poolNo;
    }

    private static PoolNo RemoveLeading(PoolNo poolNo) {
        int mask = (1 << poolNo.poolOrder)-1;
        poolNo.poolNumber = poolNo.poolNumber & mask;
        poolNo.poolOrder--;
        return poolNo;
    }

    private static PoolNo InvertAll(PoolNo poolNo) {
        poolNo.poolNumber = ~poolNo.poolNumber;
        int mask = (1 << poolNo.poolOrder) - 1;
        poolNo.poolNumber = poolNo.poolNumber & mask;
        return poolNo;
    }

    private static PoolNo ReduceOrder(PoolNo poolNo) {
        int mask = 1 << (poolNo.poolOrder-1);
        if((poolNo.poolNumber & mask) > 0)
            poolNo = InvertAll(poolNo);
        poolNo = RemoveLeading(poolNo);
        return poolNo;
    }

    private static PoolNo IncreaseOrder(PoolNo poolNo, int sign) {
        poolNo = AddLeading(poolNo);
        if(sign == 1)
            poolNo = InvertAll(poolNo);
        return poolNo;
    }

    public static List<PoolNo> IncreasePoolOrder(List<PoolNo> poolNos){

        List<PoolNo> newPoolNumbers = new ArrayList<PoolNo>();
            PoolNo poolNo0 = new PoolNo();
            for(int i = 0; i < poolNos.size(); i++){
                poolNo0 = new PoolNo();
                poolNo0.poolNumber = poolNos.get(i).poolNumber;
                poolNo0.poolOrder = poolNos.get(i).poolOrder;
                poolNo0 = IncreaseOrder(poolNo0, 0);
                newPoolNumbers.add(poolNo0);
            }
            for(int i = 0; i < poolNos.size(); i++){
                poolNo0 = new PoolNo();
                poolNo0.poolNumber = poolNos.get(i).poolNumber;
                poolNo0.poolOrder = poolNos.get(i).poolOrder;
                poolNo0 = IncreaseOrder(poolNo0, 1);
                newPoolNumbers.add(poolNo0);
            }
        return newPoolNumbers;
    }

    public static List<PoolNo> GetPoolOrder(PoolNo poolNo, int newOrder){
        List<PoolNo> poolNos = new ArrayList<PoolNo>();
        if(newOrder == GetPoolOrder(poolNo)){
            poolNos.add(poolNo);
            return poolNos;

        } else if(newOrder < GetPoolOrder(poolNo)) {
            for(int i = GetPoolOrder(poolNo); i > newOrder; i--){
                poolNo = ReduceOrder(poolNo);
            }
            poolNos.add(poolNo);
            return poolNos;

        } else {
            poolNos.add(poolNo);
            for(int i = GetPoolOrder(poolNo); i < newOrder-1; i++){
                poolNos = IncreasePoolOrder(poolNos);
            }
            return poolNos;
        }
    }

    public static List<Integer> GetFightList(int order){

        int noPlayers = OrderToNumber(order+1);
        List<Integer> playerNumbers = new ArrayList<Integer>();
        List<Integer> fightNumbers = new ArrayList<Integer>();
        List<Integer> lastFightNumbers = new ArrayList<Integer>();

        for(int i = 0; i<noPlayers/2; i++){
            fightNumbers.add(i, i);
        }
        for(int i = fightNumbers.size(); i<noPlayers; i++){
            lastFightNumbers.add(i);
        }

        Random randomGenerator = new Random();

        Map<Integer, PoolNoPath> poolNoPaths = new HashMap<Integer, PoolNoPath>();
        for(int i = 0; i<noPlayers; i++){
            poolNoPaths.put(i, GetPoolNumberPath(PoolNumber(i, order)));
        }

        Map<Integer, PoolNoPath> newpoolNoPaths = new HashMap<Integer, PoolNoPath>();

        playerNumbers.add(randomGenerator.nextInt(fightNumbers.size()));
        for(int j = 0; j < fightNumbers.size() ; j++){
            if(fightNumbers.get(j) == playerNumbers.get(0)){
                fightNumbers.remove(j);
                break;
            }
        }
        
        newpoolNoPaths.put(0, GetPoolNumberPath(PoolNumber(playerNumbers.get(0), order)));

        for(int k = 1; k < noPlayers; k++){
            int newOrder = NumberToOrder(k);
            List<Integer> newPath = new ArrayList<Integer>();
            int nextCorner = 0;

            if(fightNumbers.isEmpty())
                fightNumbers.addAll(lastFightNumbers);

            for(int i = 0; i < newOrder-1 ; i++){
                nextCorner = poolNoPaths.get(k).Corners.get(i);
                newPath.add(newpoolNoPaths.get(nextCorner).Directions.get(i));
            }
            int direction = newpoolNoPaths.get(nextCorner).Directions.get(newOrder-1) ^ 1;
            newPath.add(direction);

            PoolNo rootPoolNo = FollowPath(newPath);
            List<PoolNo> poolNos = GetPoolOrder(rootPoolNo, order);


            for(int i = 0; i < poolNos.size() ; i++){
                boolean found = false;
                for(int j = 0; j < fightNumbers.size() ; j++){
                    if(poolNos.get(i).poolNumber == fightNumbers.get(j)){
                        found = true;
                        break;
                    }
                }
                if(!found){
                    poolNos.remove(i);
                    i--;
                }
            }
            playerNumbers.add(poolNos.get(randomGenerator.nextInt(poolNos.size())).poolNumber);
            for(int j = 0; j < fightNumbers.size() ; j++){
                if(fightNumbers.get(j) == playerNumbers.get(k)){
                    fightNumbers.remove(j);
                    break;
                }
            }

            newpoolNoPaths.put(k, GetPoolNumberPath(PoolNumber(playerNumbers.get(k), order)));

        }
        return playerNumbers;
    }

    public static Map<Integer, Integer> SeedToPoolMap(int noPlayers){
        int order = NumberToOrder(noPlayers-1);
        Map<Integer, Integer> seedToPoolMap = new HashMap<Integer, Integer>();
        List<Integer> seedToPool = GetFightList(order);
        int position = 0;
        for(int i = 0; i < seedToPool.size(); i++){
            if(seedToPool.get(i) < noPlayers){
                seedToPoolMap.put(position, seedToPool.get(i));
                position ++;
            }
        }
        return seedToPoolMap;
    }

    public static Map<Integer, Integer> PlayerIDToScore(Map<Integer, Integer> playerIDToPoolNo, int noPlayers ){

        int order = NumberToOrder(noPlayers-1);
        Map<Integer, Integer> playerIDToScore = new HashMap<Integer, Integer>();
        Map<Integer, PoolNoPath> playerIDToPoolNoPath = new HashMap<Integer, PoolNoPath>();
        
        for(Integer playerID : playerIDToPoolNo.keySet()) {
            PoolNo poolNo = PoolNumber(playerIDToPoolNo.get(playerID), order);
            PoolNoPath poolNoPath = GetPoolNumberPath(poolNo);
            playerIDToPoolNoPath.put(playerID, poolNoPath);
        }
        
        for(Integer playerID : playerIDToPoolNo.keySet()) {
            List<Integer> corners = playerIDToPoolNoPath.get(playerID).Corners;
            int Score = 1;
            for(Integer compareToPlayerID : playerIDToPoolNo.keySet()) {
                if(playerID != compareToPlayerID){
                    List<Integer> compareTocorners = playerIDToPoolNoPath.get(compareToPlayerID).Corners;
                    for(int i = 0 ; i < corners.size(); i++){
                        if(corners.get(i) == compareTocorners.get(i))
                            Score = Score + 1;
                    }
                }
            }
            if(playerIDToPoolNo.size() == 1)
                Score = 0;  // I realise this is shit but it works
            playerIDToScore.put(playerID, Score);
        }

        return playerIDToScore;
    }


    public static Map<Integer, Integer> PlayerScores(Map<Integer, Integer> playerIDToPoolNo, Map<String, List<Integer>> teamToPlayerID, int numberOfPlayerPositions ){

        Map<Integer, Integer> playerIDToScore = new HashMap<Integer, Integer>();
        for(String team : teamToPlayerID.keySet()) {
            Map<Integer, Integer> teamPlayerIDToPoolNo = new HashMap<Integer, Integer>();
            for(int playerID : teamToPlayerID.get(team)){
                teamPlayerIDToPoolNo.put(playerID, playerIDToPoolNo.get(playerID));
            }
            Map<Integer, Integer> teamPlayerIDToScore = PoolNumber.PlayerIDToScore(teamPlayerIDToPoolNo, numberOfPlayerPositions );
            playerIDToScore.putAll(teamPlayerIDToScore);
        }
        return playerIDToScore;
    }


    public static Map<Integer, Integer> SeperateTeams(Map<Integer, Integer> poolNoToPlayerID, Map<String, List<Integer>> teamToPlayerID, Map<Integer, Integer> playerIDToSeed, int numberOfPlayerPositions ){

        Map<Integer, Integer> playerIDToPoolNo = new HashMap<Integer, Integer>();
        for(int poolNo : poolNoToPlayerID.keySet()) {
            if(poolNoToPlayerID.get(poolNo) != null){
                int playerID = poolNoToPlayerID.get(poolNo);
                playerIDToPoolNo.put(playerID, poolNo);
                if(!playerIDToSeed.containsKey(playerID)){
                    playerIDToSeed.put(playerID, 0);
                }
            }
        }
        Map<Integer, Integer> playerIDToScore = PlayerScores(playerIDToPoolNo, teamToPlayerID, numberOfPlayerPositions );
        for(int poolNo1 : poolNoToPlayerID.keySet()) {
            for(int poolNo2 : poolNoToPlayerID.keySet()) {
                if(poolNo1 != poolNo2) {
                    Integer playerID1 = poolNoToPlayerID.get(poolNo1);
                    Integer playerID2 = poolNoToPlayerID.get(poolNo2);
                    if ((playerID1 == null && playerID2 != null && playerIDToSeed.get(playerID2).equals(0)) 
                            || (playerID1 != null && playerID2 == null && playerIDToSeed.get(playerID1).equals(0)) 
                            || (playerID1 != null && playerID2 != null && playerIDToSeed.get(playerID1).equals(playerIDToSeed.get(playerID2)))) {

                        Map<Integer, Integer> newPoolNoToPlayerID = new HashMap<Integer, Integer>(poolNoToPlayerID);
                        newPoolNoToPlayerID.put(poolNo1, poolNoToPlayerID.get(poolNo2));
                        newPoolNoToPlayerID.put(poolNo2, poolNoToPlayerID.get(poolNo1));

                        playerIDToPoolNo.clear();
                        for(int poolNo : newPoolNoToPlayerID.keySet()) {
                            if(newPoolNoToPlayerID.get(poolNo) != null){
                                playerIDToPoolNo.put(newPoolNoToPlayerID.get(poolNo), poolNo);
                            }
                        }

                        Map<Integer, Integer> newPlayerIDToScore = PlayerScores(playerIDToPoolNo, teamToPlayerID, numberOfPlayerPositions );

                        int oldScore = 0;
                        int score = -1;
                        if(playerID1 == null && playerID2 != null){
                            oldScore = playerIDToScore.get(playerID2);
                            score = newPlayerIDToScore.get(playerID2);
                        }else if(playerID1 != null && playerID2 == null){
                            oldScore = playerIDToScore.get(playerID1);
                            score = newPlayerIDToScore.get(playerID1);
                        } else if(playerID1 != null && playerID2 != null) {
                            oldScore = Math.max(playerIDToScore.get(playerID1) , playerIDToScore.get(playerID2));
                            score = Math.max(newPlayerIDToScore.get(playerID1) , newPlayerIDToScore.get(playerID2));
                        }
                        if(score < oldScore){
                            playerIDToScore = newPlayerIDToScore;
                            poolNoToPlayerID = newPoolNoToPlayerID;
                        }
                    }
                }
            }
        }
        return poolNoToPlayerID;
    }

}
