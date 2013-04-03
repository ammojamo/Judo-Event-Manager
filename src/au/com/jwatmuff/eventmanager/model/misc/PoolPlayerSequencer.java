/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.misc;

import au.com.jwatmuff.eventmanager.db.FightDAO;
import au.com.jwatmuff.eventmanager.model.info.PlayerPoolInfo;
import au.com.jwatmuff.eventmanager.model.vo.Fight;
import au.com.jwatmuff.eventmanager.model.vo.PlayerPool;
import au.com.jwatmuff.genericdb.Database;
import au.com.jwatmuff.genericdb.transaction.Transaction;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class PoolPlayerSequencer {
    private static final Logger log = Logger.getLogger(PoolPlayerSequencer.class);

    private static Map<Database, PoolPlayerSequencer> instances = new HashMap<Database, PoolPlayerSequencer>();

    private static final Comparator<PlayerPoolInfo> PLAYERS_COMPARATOR = new Comparator<PlayerPoolInfo>() {
        @Override
        public int compare(PlayerPoolInfo p1, PlayerPoolInfo p2) {
            String n1 = p1.getPlayer().getLastName() + p1.getPlayer().getFirstName();
            String n2 = p2.getPlayer().getLastName() + p2.getPlayer().getFirstName();
            return n1.compareTo(n2);
        }
    };

    private static final Comparator<PlayerPoolInfo> PLAYERS_COMPARATOR_POSITION = new Comparator<PlayerPoolInfo>() {
        @Override
        public int compare(PlayerPoolInfo pp1, PlayerPoolInfo pp2) {
            return pp1.getPlayerPool().getPlayerPosition() - pp2.getPlayerPool().getPlayerPosition();
        }
    };
    
    /*
    private static class PoolFightPlayerInfo extends ArrayList<Set<Integer>> {}
    private Map<Integer, PoolFightPlayerInfo> poolFightPlayerInfo = new HashMap<Integer, PoolFightPlayerInfo>();
    */
    
    private Map<Integer, List<Fight>> poolFights = new HashMap<Integer, List<Fight>>();

    private PoolPlayerSequencer() {
    }
    
    private static PoolPlayerSequencer getInstance(Database database) {
        if(!instances.containsKey(database))
            instances.put(database, new PoolPlayerSequencer());
        return instances.get(database);
    }

//    public static boolean hasCommonPlayers(Database database, int poolID, int[] fightPositions) {
//        PoolPlayerSequencer pps = getInstance(database);
//
//        if(!pps.poolFights.containsKey(poolID)) {
//            Pool p = database.get(Pool.class, poolID);
//            if(p.getLockedStatus() != Pool.LockedStatus.FIGHTS_LOCKED)
//                return false;
//
//            List<Fight> fights = database.findAll(Fight.class, FightDAO.FOR_POOL, poolID);
//            pps.poolFights.put(poolID, fights);
//        }
//
//        List<Fight> fights = pps.poolFights.get(poolID);
//
//        int i = fightPositions[0];
//        int j = fightPositions[1];
//        if(i < j) { int k = i; i = j; j = k; } //swap
//
//        for(String code : fights.get(i-1).getPlayerCodes()) {
//            if(PlayerCodeParser.getPrefix(code).equals("P")) {
//                for(String code2 : fights.get(j-1).getPlayerCodes())
//                    if(code.equals(code2)) return true;
//            } else {
//                int n = PlayerCodeParser.getNumber(code);
//                if(n == j) return true;
//            }
//        }
//
//        return false;
//    }

//    public static ArrayList<Integer> getDependentFights(Database database, int poolID, int fightPosition) {
//        PoolPlayerSequencer pps = getInstance(database);
//        ArrayList<Integer> dependentFights = new ArrayList<Integer>();
//        dependentFights.add(fightPosition);
//
//        if(!pps.poolFights.containsKey(poolID)) {
//            Pool p = database.get(Pool.class, poolID);
//            if(p.getLockedStatus() != Pool.LockedStatus.FIGHTS_LOCKED){
//                System.out.println("***I don't think this should happen");
//                return null;
//            }
//
//            List<Fight> fights = database.findAll(Fight.class, FightDAO.FOR_POOL, poolID);
//            pps.poolFights.put(poolID, fights);
//        }
//
//        List<Fight> fights = pps.poolFights.get(poolID);
//
//        for(String code : fights.get(fightPosition-1).getPlayerCodes()) {
//            List<Integer> ascendantNumbers = PlayerCodeParser.getAscendant(code);
//            for(Integer ascendantNumber : ascendantNumbers) {
//                dependentFights.addAll(getDependentFights(database, poolID, ascendantNumber));
//            }
//        }
//
//        return dependentFights;
//    }

    public static ArrayList<Integer> getSameTeamFights(Database database, int poolID) {
        PlayerCodeParser parser = PlayerCodeParser.getInstance(database, poolID);
        ArrayList<Integer> sameTeamFights = parser.getSameTeamFightsFirst();
        return sameTeamFights;
    }

//    public static ArrayList<Player> getPossiblePlayers(Database database, int poolID, int fightPosition) {
//        PoolPlayerSequencer pps = getInstance(database);
//        ArrayList<Player> possiblePlayers = new ArrayList<Player>();
//        PlayerCodeParser parser = PlayerCodeParser.getInstance(database, poolID);
//
//        if(!pps.poolFights.containsKey(poolID)) {
//            Pool p = database.get(Pool.class, poolID);
//            if(p.getLockedStatus() != Pool.LockedStatus.FIGHTS_LOCKED)
//                return null;
//
//            List<Fight> fights = database.findAll(Fight.class, FightDAO.FOR_POOL, poolID);
//            pps.poolFights.put(poolID, fights);
//        }
//
//        List<Fight> fights = pps.poolFights.get(poolID);
//
//        for(String code : fights.get(fightPosition-1).getPlayerCodes()) {
//            if(PlayerCodeParser.getCodeInfo(code).type == PlayerCodeParser.CodeType.PLAYER) {
//                FightPlayer tempPlayer = parser.parseCode(code);
//                if(tempPlayer.type == PlayerCodeParser.PlayerType.NORMAL)
//                    possiblePlayers.add(tempPlayer.player);
//            } else {
//                for(int newNumber : PlayerCodeParser.getAscendant(code))
//                    possiblePlayers.addAll(getPossiblePlayers(database, poolID, newNumber ));
//            }
//        }
//        return possiblePlayers;
//    }

    public static List<PlayerPoolInfo> getPlayerSequence(Database database, int poolID) {
        int numPlayerPositions = getNumberOfPlayerPositions(database, poolID);
        List<PlayerPoolInfo> players = new ArrayList<PlayerPoolInfo>(PlayerPoolInfo.getForPool(database, poolID));
        List<PlayerPoolInfo> newPlayers = new ArrayList<PlayerPoolInfo>();

        CollectionUtils.filter(players, new Predicate() {
            @Override
            @SuppressWarnings("unchecked")
            public boolean evaluate(Object player) {
                return ((PlayerPoolInfo)player).getPlayerPool().isApproved();
            }
        });

        boolean repositionPlayers = false;
        for(PlayerPoolInfo player : players) {
            if(player.getPlayerPool().getPlayerPosition() > numPlayerPositions) {
                repositionPlayers = true;
            }
        }

        if(repositionPlayers) {
            Collections.sort(players, PLAYERS_COMPARATOR_POSITION);
            newPlayers.addAll(players);
        } else {
            Collections.sort(players, PLAYERS_COMPARATOR_POSITION);

            // create a new list with null entries so that player pool position reflects
            // the index of each player in the list
            int index = 1;
            for(PlayerPoolInfo player : players) {
                while(index < player.getPlayerPool().getPlayerPosition()) {
                    newPlayers.add(null);
                    index++;
                }
                newPlayers.add(player);
                index++;
            }
        }

        // finally, insert any additional nulls to make the length of the list
        // equal to the number of player positions in the pool
        for(int index = newPlayers.size()+1; index <= numPlayerPositions; index++)
            newPlayers.add(null);

        return newPlayers;
    }
    
    public static void savePlayerSequence(final TransactionalDatabase database, int poolID, final List<PlayerPoolInfo> playerPoolInfolist) {
        database.perform(new Transaction() {

            @Override
            public void perform() {
                int pos = 0;
                for(PlayerPoolInfo playerPoolInfo : playerPoolInfolist) {
                    pos++;
                    if(playerPoolInfo != null) {
                        PlayerPool playerPool = playerPoolInfo.getPlayerPool();
                        if(playerPool.getPlayerPosition() != pos) {
                            playerPool.setPlayerPosition(pos);
                            database.update(playerPool);
                        }
                    }
                }
            }
        });
    }

    public static int getNumberOfPlayerPositions(Database database, int poolID) {
        List<Fight> fights = database.findAll(Fight.class, FightDAO.FOR_POOL, poolID);
        int max = 0;
        for(Fight fight : fights) {
            for(String code : fight.getPlayerCodes()) {
                if(code.startsWith("P")) {
                    try {
                        max = Math.max(max, Integer.parseInt(code.substring(1)));
                    } catch(NumberFormatException e) {
                        log.warn("Number format exception while parsing code: " + code);
                    }
                }
            }
        }
        return max;
    }
}
