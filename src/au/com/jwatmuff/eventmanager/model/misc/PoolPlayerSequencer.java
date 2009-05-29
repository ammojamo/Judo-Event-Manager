/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.misc;

import au.com.jwatmuff.eventmanager.db.FightDAO;
import au.com.jwatmuff.eventmanager.model.info.PlayerPoolInfo;
import au.com.jwatmuff.eventmanager.model.vo.Fight;
import au.com.jwatmuff.eventmanager.model.vo.PlayerPool;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
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

/**
 *
 * @author James
 */
public class PoolPlayerSequencer {
    private static Map<Database, PoolPlayerSequencer> instances = new HashMap<Database, PoolPlayerSequencer>();
    
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
    
    public static boolean hasCommonPlayers(Database database, int poolID, int[] fightPositions) {
        PoolPlayerSequencer pps = getInstance(database);
        
        if(!pps.poolFights.containsKey(poolID)) {
            Pool p = database.get(Pool.class, poolID);
            if(p.getLockedStatus() != Pool.LockedStatus.FIGHTS_LOCKED)
                return false;

            List<Fight> fights = database.findAll(Fight.class, FightDAO.FOR_POOL, poolID);
            pps.poolFights.put(poolID, fights);
        }
        
        List<Fight> fights = pps.poolFights.get(poolID);
        
        int i = fightPositions[0];
        int j = fightPositions[1];
        if(i < j) { int k = i; i = j; j = k; } //swap
        
        for(String code : fights.get(i-1).getPlayerCodes()) {
            if(PlayerCodeParser.getPrefix(code).equals("P")) {
                for(String code2 : fights.get(j-1).getPlayerCodes())
                    if(code.equals(code2)) return true;
            } else {
                int n = PlayerCodeParser.getNumber(code);
                if(n == j) return true;
            }
        }

        return false;
    }
    
    /*
    public static boolean hasCommonPlayers(Database database, int poolID, int[] fightPositions) {
        PoolPlayerSequencer pps = getInstance(database);
        
        if(!pps.poolFightPlayerInfo.containsKey(poolID)) {
            Pool p = database.get(Pool.class, poolID);
            if(p.getLockedStatus() != Pool.LockedStatus.FIGHTS_LOCKED)
                return false;
            
            PoolFightPlayerInfo fightPlayerInfo = new PoolFightPlayerInfo();

            List<Fight> fights = database.findAll(Fight.class, FightDAO.FOR_POOL, poolID);
            for(Fight fight : fights) {
                Set<Integer> possiblePlayers = new HashSet<Integer>();
                for(String code : fight.getPlayerCodes()) {
                    String prefix = PlayerCodeParser.getPrefix(code);
                    int number = PlayerCodeParser.getNumber(code);
                    if(prefix.equals("P")) {
                        possiblePlayers.add(number);
                    } else {
                        possiblePlayers.addAll(fightPlayerInfo.get(number-1));
                    }
                }
                fightPlayerInfo.add(possiblePlayers);
                System.out.print("F" + fight.getPosition());
                for(int i : possiblePlayers) {
                    System.out.print(i + ", ");
                }
                System.out.println();
            }
            pps.poolFightPlayerInfo.put(poolID, fightPlayerInfo);
        }
        
        PoolFightPlayerInfo info = pps.poolFightPlayerInfo.get(poolID);
        
        return CollectionUtils.containsAny(info.get(fightPositions[0]-1), info.get(fightPositions[1]-1));
    }
    */
    
    
    public static List<PlayerPoolInfo> getPlayerSequence(Database database, int poolID) {
        List<PlayerPoolInfo> players = new ArrayList<PlayerPoolInfo>(PlayerPoolInfo.getForPool(database, poolID));

        CollectionUtils.filter(players, new Predicate() {
            @Override
            @SuppressWarnings("unchecked")
            public boolean evaluate(Object player) {
                return ((PlayerPoolInfo)player).getPlayerPool().isApproved();
            }
        });
        
        Collections.sort(players, new Comparator<PlayerPoolInfo> () {
            @Override
            public int compare(PlayerPoolInfo pp1, PlayerPoolInfo pp2) {
                return pp1.getPlayerPool().getPlayerPosition() - pp2.getPlayerPool().getPlayerPosition();
            }
        });
        
        return players;
    }
    
    public static void savePlayerSequence(final TransactionalDatabase database, int poolID, final List<PlayerPoolInfo> players) {
        database.perform(new Transaction() {

            @Override
            public void perform() {
                int pos = 0;
                for(PlayerPoolInfo player : players) {
                    pos++;
                    PlayerPool pp = player.getPlayerPool();
                    if(pp.getPlayerPosition() != pos) {
                        pp.setPlayerPosition(pos);
                        database.update(pp);
                    }
                }
            }
        });
    }

}
