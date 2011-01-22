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
        for(PlayerPoolInfo player : playerPoolInfoList)
            if(player != null && player.getPlayer().getID() == id)
                return player;
        return null;
    }


    private List<PlayerPoolInfo> orderPlayers() {
        /********************************************************************************/
        /* Ordering of players based on seeds - TODO: it might be possible to simplify this :) yes I think so */
        /* TODO: if this code needs to be used elsewhere, it should be moved to a shared utility class */
        /* TODO: This should be moved to a shared utility class to ensure easy update and inspection*/
        /********************************************************************************/

        int numPlayers = getNumberOfPlayerPositions();

        // create a list of all players, unordered
        List<PlayerPoolInfo> unorderedPlayers = new ArrayList<PlayerPoolInfo>(playerPoolInfoList);

        // add null (bye) players to fill the available positions in the draw
        while(unorderedPlayers.size() < numPlayers/2)
            unorderedPlayers.add(null);

        // create a list to hold the players after they have been ordered
        List<PlayerPoolInfo> orderedPlayers = new ArrayList<PlayerPoolInfo>();

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
                    unorderedPlayers.remove(player);
                    orderedPlayers.add(player);
                }
            }
        }

        // randomize remaining unordered players
        Collections.shuffle(unorderedPlayers);

        // fill at least half the available position in the ordered players list
        Iterator<PlayerPoolInfo> unorderedPlayersIterator = unorderedPlayers.iterator();
        while(orderedPlayers.size() < numPlayers/2 && unorderedPlayersIterator.hasNext()) {
            PlayerPoolInfo uoPlayer = unorderedPlayersIterator.next();
                orderedPlayers.add(uoPlayer);
        }
        unorderedPlayers.removeAll(orderedPlayers);

        // add null (bye) players to fill the available positions in the draw
        while(unorderedPlayers.size() + orderedPlayers.size() < numPlayers)
            unorderedPlayers.add(null);

        // randomize remaining unordered players, and add them to the ordered players
        Collections.shuffle(unorderedPlayers);

        orderedPlayers.addAll(unorderedPlayers);

        return orderedPlayers;
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
