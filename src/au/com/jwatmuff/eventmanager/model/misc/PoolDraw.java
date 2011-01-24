/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.misc;

import au.com.jwatmuff.eventmanager.db.FightDAO;
import au.com.jwatmuff.eventmanager.model.info.PlayerPoolInfo;
import au.com.jwatmuff.eventmanager.model.vo.Fight;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;


/**
 *
 * @author Leonard Hall
 */
public class PoolDraw {
    private static final Logger log = Logger.getLogger(PoolDraw.class);

    private Pool pool;
    private List<PlayerPoolInfo> playerPoolInfoList;
    private List<Fight> fights;
    private Map<Integer, Integer> seeds;

    private int getNumberOfPlayerPositions() {
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

    private PlayerPoolInfo getPlayerById(int id) {
        for(PlayerPoolInfo playerPoolInfo : playerPoolInfoList)
            if(playerPoolInfo != null && playerPoolInfo.getPlayer().getID() == id)
                return playerPoolInfo;
        return null;
    }


    private List<PlayerPoolInfo> orderPlayers() {
        /********************************************************************************/
        /* Ordering of players based on seeds - TODO: it might be possible to simplify this :) yes I think so */
        /* TODO: if this code needs to be used elsewhere, it should be moved to a shared utility class */
        /* TODO: This should be moved to a shared utility class to ensure easy update and inspection*/
        /********************************************************************************/

//
//        int noPlayers = 16;
//        List<Integer> playerNumbers = PoolNumber.GetFightList(noPlayers);
//System.out.println("*****************************************************************");
//System.out.println("Places **********************************************************");
//        for(int i = 0; i<noPlayers; i++){
//            System.out.println("Player " + i + "     Place " + playerNumbers.get(i) );
//        }


//System.out.println("Corners *****************************************************************");
//        for(int i = 0; i<order; i++){
//            System.out.println("newPoolNoPath0 " + newpoolNoPaths.get(0).Corners.get(i) + "     newPoolNoPath1 " + newpoolNoPaths.get(1).Corners.get(i) + "     newPoolNoPath2 " + newpoolNoPaths.get(2).Corners.get(i) + "     newPoolNoPath3 " + newpoolNoPaths.get(3).Corners.get(i) + "     poolNoPath0 " + poolNoPaths.get(0).Corners.get(i) + "     poolNoPath1 " + poolNoPaths.get(1).Corners.get(i) + "     poolNoPath2 " + poolNoPaths.get(2).Corners.get(i) + "     poolNoPath3 " + poolNoPaths.get(3).Corners.get(i) );
//        }
//System.out.println("Directions *****************************************************************");
//        for(int i = 0; i<order; i++){
//            System.out.println("newPoolNoPath0 " + newpoolNoPaths.get(0).Directions.get(i) + "     newPoolNoPath1 " + newpoolNoPaths.get(1).Directions.get(i) + "     newPoolNoPath2 " + newpoolNoPaths.get(2).Directions.get(i) + "     newPoolNoPath3 " + newpoolNoPaths.get(3).Directions.get(i) + "     poolNoPath0 " + poolNoPaths.get(0).Directions.get(i) + "     poolNoPath1 " + poolNoPaths.get(1).Directions.get(i) + "     poolNoPath2 " + poolNoPaths.get(2).Directions.get(i) + "     poolNoPath3 " + poolNoPaths.get(3).Directions.get(i) );
//        }



        int numPlayers = getNumberOfPlayerPositions();

        // create a list of all players, unordered
        List<PlayerPoolInfo> unorderedPlayerPoolInfo = new ArrayList<PlayerPoolInfo>(playerPoolInfoList);

        // add null (bye) players to fill the available positions in the draw
        while(unorderedPlayerPoolInfo.size() < numPlayers/2)
            unorderedPlayerPoolInfo.add(null);

        // create a list to hold the players after they have been ordered
        List<PlayerPoolInfo> seedOrderPlayerPoolInfo = new ArrayList<PlayerPoolInfo>();

        // create a list to hold the players after pool positions are converted to player positions
        List<PlayerPoolInfo> finalPlayerPoolInfo = new ArrayList<PlayerPoolInfo>(numPlayers);

        // get the set of all seeds specified, in order from lowest to highest
        List<Integer> seedSet = new ArrayList<Integer>();
        for(Integer seed : seeds.values())
            if(seed != null && !seedSet.contains(seed))
                seedSet.add(seed);

        Collections.sort(seedSet);

        // build up ordered list of players from those players that had seeds specified
        for(Integer seed : seedSet) {
            for(Integer playerID : seeds.keySet()) {
                if(seeds.get(playerID) == seed) {
                    PlayerPoolInfo player = getPlayerById(playerID);
                    unorderedPlayerPoolInfo.remove(player);
                    seedOrderPlayerPoolInfo.add(player);
                }
            }
        }

        // randomize remaining unordered players
        Collections.shuffle(unorderedPlayerPoolInfo);

        // fill at least half the available position in the ordered players list
        Iterator<PlayerPoolInfo> unorderedPlayersIterator = unorderedPlayerPoolInfo.iterator();
        while(seedOrderPlayerPoolInfo.size() < numPlayers/2 && unorderedPlayersIterator.hasNext()) {
            PlayerPoolInfo unorderedPlayer = unorderedPlayersIterator.next();
                seedOrderPlayerPoolInfo.add(unorderedPlayer);
        }
        unorderedPlayerPoolInfo.removeAll(seedOrderPlayerPoolInfo);

        // add null (bye) players to fill the available positions in the draw
        while(unorderedPlayerPoolInfo.size() + seedOrderPlayerPoolInfo.size() < numPlayers)
            unorderedPlayerPoolInfo.add(null);

        // randomize remaining unordered players, and add them to the ordered players
        Collections.shuffle(unorderedPlayerPoolInfo);

        seedOrderPlayerPoolInfo.addAll(unorderedPlayerPoolInfo);

        Map<Integer, Integer> seedToPoolMap = PoolNumber.SeedToPoolMap(numPlayers);
        Map<Integer, Integer> poolToPositionMap = pool.getDrawPools();
        Map<Integer, Integer> finalPlayerPoolInfoMap = new HashMap<Integer, Integer>();

        for(int i = 0; i < numPlayers; i++ ){
            int PoolPosition = seedToPoolMap.get(i);
            int PlayerPosition = poolToPositionMap.get(PoolPosition+1)-1;
            finalPlayerPoolInfoMap.put(PlayerPosition, i);
        }

        for(int i = 0; i < numPlayers; i++ ){
            finalPlayerPoolInfo.add(i, seedOrderPlayerPoolInfo.get(finalPlayerPoolInfoMap.get(i)));
        }
        return finalPlayerPoolInfo;
    }

    /* Parser instance stuff */

    private PoolDraw(TransactionalDatabase database, int poolID, Map<Integer, Integer> seeds) {
        pool = database.get(Pool.class, poolID);
        playerPoolInfoList = PoolPlayerSequencer.getPlayerSequence(database, poolID);
        fights = database.findAll(Fight.class, FightDAO.FOR_POOL, pool.getID());
        this.seeds = seeds;
    }

    private PoolDraw(TransactionalDatabase database, int poolID) {
        pool = database.get(Pool.class, poolID);
        playerPoolInfoList = PoolPlayerSequencer.getPlayerSequence(database, poolID);
        fights = database.findAll(Fight.class, FightDAO.FOR_POOL, pool.getID());
    }

    public static PoolDraw getInstance(TransactionalDatabase database, int poolID, Map<Integer, Integer> seeds) {
        PoolDraw poolDraw = new PoolDraw(database, poolID, seeds);
        return poolDraw;
    }

    public static PoolDraw getInstanceNoSeeding(TransactionalDatabase database, int poolID) {
        PoolDraw poolDraw = new PoolDraw(database, poolID);
        return poolDraw;
    }

    public List<PlayerPoolInfo> getOrderedPlayers() {
        return orderPlayers();
    }

}
