/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.jwatmuff.eventmanager.model.misc;

import au.com.jwatmuff.eventmanager.db.FightDAO;
import au.com.jwatmuff.eventmanager.model.config.ConfigurationFile;
import au.com.jwatmuff.eventmanager.model.info.PlayerPoolInfo;
import au.com.jwatmuff.eventmanager.model.vo.CompetitionInfo;
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
    private ConfigurationFile configurationFile;

    private int getNumberOfPlayerPositions() {
        int max = 0;
        for (Fight fight : fights) {
            for (String code : fight.getPlayerCodes()) {
                if (code.startsWith("P")) {
                    try {
                        max = Math.max(max, Integer.parseInt(code.substring(1)));
                    } catch (NumberFormatException e) {
                        log.warn("Number format exception while parsing code: " + code);
                    }
                }
            }
        }
        return max;
    }

    private PlayerPoolInfo getPlayerById(int id) {
        for (PlayerPoolInfo playerPoolInfo : playerPoolInfoList) {
            if (playerPoolInfo != null && playerPoolInfo.getPlayer().getID() == id) {
                return playerPoolInfo;
            }
        }
        return null;
    }

    private List<PlayerPoolInfo> orderPlayers() {
        /********************************************************************************/
        /* Ordering of players based on seeds - TODO: it might be possible to simplify this :) yes I think so */
        /* TODO: if this code needs to be used elsewhere, it should be moved to a shared utility class */
        /* TODO: This should be moved to a shared utility class to ensure easy update and inspection*/
        /********************************************************************************/
        /********************************************************************************/
        /* This method may be improved by not adding the randome position in the form of seedToPoolMap*/
        /* for divisions with non power of 2.... maybe*/
        /********************************************************************************/
        int numberOfPlayerPositions = getNumberOfPlayerPositions();

//Seeds is Map of PlayerID to seed

//Map of seed to pool
        Map<Integer, Integer> seedToPoolMap = PoolNumber.SeedToPoolMap(numberOfPlayerPositions);

//Map of pool to position
        Map<Integer, Integer> positionToPoolMap = pool.getDrawPools();

//Map of seed number to playerID array
        Map<Integer, List<Integer>> seedToPlayerID = new HashMap<Integer, List<Integer>>();

//Map of team to playerID array
        Map<String, List<Integer>> teamToPlayerID = new HashMap<String, List<Integer>>();

// create a list of all players, unordered
        List<PlayerPoolInfo> unorderedPLayers = new ArrayList<PlayerPoolInfo>(playerPoolInfoList);

// create a list to hold the players after they have been ordered
        List<PlayerPoolInfo> seededPlayers = new ArrayList<PlayerPoolInfo>();

// create a list to hold the players after they have been ordered to seperate like teams
        List<PlayerPoolInfo> teamSeededPlayers = new ArrayList<PlayerPoolInfo>();

// create a list to hold the players after pool positions are converted to player positions
        List<PlayerPoolInfo> positionedPlayers = new ArrayList<PlayerPoolInfo>();

        for (PlayerPoolInfo playerPoolInfo : playerPoolInfoList) {
            if (playerPoolInfo != null) {
                int playerID = playerPoolInfo.getPlayer().getID();
                String team = playerPoolInfo.getPlayer().getTeam();
                if (team != null) {
                    List<Integer> playerIDArray = new ArrayList<Integer>();
                    if (!teamToPlayerID.containsKey(team)) {
                        playerIDArray.add(playerID);
                        teamToPlayerID.put(team, playerIDArray);
                    } else {
                        playerIDArray = teamToPlayerID.get(team);
                        playerIDArray.add(playerID);
                        teamToPlayerID.put(team, playerIDArray);
                    }
                }
            }
        }

// get the set of all seeds specified, in order from lowest to highest
// Creat a list of seed values
        List<Integer> seedSet = new ArrayList<Integer>();
        for (Integer seed : seeds.values()) {
            if (seed != null && seed != 0 && !seedSet.contains(seed)) {
                seedSet.add(seed);
            }
        }

        Collections.sort(seedSet);

// build up ordered list of players from those players that had seeds specified
        for (Integer seed : seedSet) {
            List<PlayerPoolInfo> seedPlayers = new ArrayList<PlayerPoolInfo>();
            List<Integer> playerIDs = new ArrayList<Integer>();
            playerIDs.addAll(seeds.keySet());
            Collections.shuffle(playerIDs);
            for (Integer playerID : playerIDs) {
                if (seeds.get(playerID) == seed) {
// fill in a seed to playerID list map
                    List<Integer> seedArray = new ArrayList<Integer>();
                    if (!seedToPlayerID.containsKey(seed)) {
                        seedArray.add(playerID);
                        seedToPlayerID.put(seed, seedArray);
                    } else {
                        seedArray = seedToPlayerID.get(seed);
                        seedArray.add(playerID);
                        seedToPlayerID.put(seed, seedArray);
                    }

                    PlayerPoolInfo player = getPlayerById(playerID);
                    seedPlayers.add(player);
                }
// ensure that like seeds are randomised
            }
            Collections.shuffle(seedPlayers);
            seededPlayers.addAll(seedPlayers);
            unorderedPLayers.removeAll(seedPlayers);
//            seedPlayers.clear(); // not needed I think
        }

// randomize remaining unordered players
        Collections.shuffle(unorderedPLayers);

        String byePlacement = configurationFile.getProperty("defaultByePlacement");

        if (byePlacement.contentEquals("Balenced")) {
// fill at least half the available position in the ordered players list
            Iterator<PlayerPoolInfo> unorderedPlayersIterator = unorderedPLayers.iterator();
            while (seededPlayers.size() < numberOfPlayerPositions / 2 && unorderedPlayersIterator.hasNext()) {
                PlayerPoolInfo unorderedPlayer = unorderedPlayersIterator.next();
                if (unorderedPlayer != null) {
                    seededPlayers.add(unorderedPlayer);
                }
            }
            
            List<PlayerPoolInfo> byeSeededPlayers = new ArrayList<PlayerPoolInfo>();
            while (unorderedPlayersIterator.hasNext()) {
                PlayerPoolInfo unorderedPlayer = unorderedPlayersIterator.next();
                if (unorderedPlayer != null) {
                    byeSeededPlayers.add(unorderedPlayer);
                }
            }

// add null (bye) players to fill the available positions in the draw
            while (byeSeededPlayers.size() + seededPlayers.size() < numberOfPlayerPositions) {
                byeSeededPlayers.add(null);
            }

            Map<Integer, Integer> byeSeedToPoolMap = PoolNumber.SeedToPoolMap(byeSeededPlayers.size());
            
            for (int seedNo = 0; seedNo < byeSeededPlayers.size(); seedNo++) {
                int playerLocation = byeSeedToPoolMap.get(seedNo);
    //            int poolLocation = seedNo;
                seededPlayers.add(byeSeededPlayers.get(playerLocation));
            }

        } else if (byePlacement.contentEquals("SeededLast")) {
// fill all available position in the ordered players list
            Iterator<PlayerPoolInfo> unorderedPlayersIterator = unorderedPLayers.iterator();
            while (seededPlayers.size() < numberOfPlayerPositions && unorderedPlayersIterator.hasNext()) {
                PlayerPoolInfo unorderedPlayer = unorderedPlayersIterator.next();
                if (unorderedPlayer != null) {
                    seededPlayers.add(unorderedPlayer);
                }
            }
            unorderedPLayers.removeAll(seededPlayers);

// add null (bye) players to fill the available positions in the draw
            while (seededPlayers.size() < numberOfPlayerPositions) {
                seededPlayers.add(null);
            }

        } else {
// fill at least half the available position in the ordered players list
            Iterator<PlayerPoolInfo> unorderedPlayersIterator = unorderedPLayers.iterator();
            while (seededPlayers.size() < numberOfPlayerPositions / 2 && unorderedPlayersIterator.hasNext()) {
                PlayerPoolInfo unorderedPlayer = unorderedPlayersIterator.next();
                if (unorderedPlayer != null) {
                    seededPlayers.add(unorderedPlayer);
                }
            }
            unorderedPLayers.removeAll(seededPlayers);

// add null (bye) players to fill the available positions in the draw
            while (unorderedPLayers.size() + seededPlayers.size() < numberOfPlayerPositions) {
                unorderedPLayers.add(null);
            }

// randomize remaining unordered players, and add them to the ordered players
            Collections.shuffle(unorderedPLayers);
            seededPlayers.addAll(unorderedPLayers);

        }

// Create Pool to PlayerID map
        Map<Integer, Integer> poolNoToPlayerID = new HashMap<Integer, Integer>();
        for (int playerPos = 0; playerPos < numberOfPlayerPositions; playerPos++) {
            if (seededPlayers.get(playerPos) != null) {
                poolNoToPlayerID.put(playerPos, seededPlayers.get(playerPos).getPlayer().getID());
            } else {
                poolNoToPlayerID.put(playerPos, null);
            }
        }

// Check for Team disadvantage in seeds
        if (configurationFile.getBooleanProperty("seperateTeamMates", true)) {
            poolNoToPlayerID = PoolNumber.SeperateTeams(poolNoToPlayerID, teamToPlayerID, seeds, numberOfPlayerPositions);
        }

// Convert map back to list
        for (int playerPos = 0; playerPos < numberOfPlayerPositions; playerPos++) {
            Integer playerID = poolNoToPlayerID.get(playerPos);
            if (playerID != null) {
                for (PlayerPoolInfo playerPoolInfo : seededPlayers) {
                    if (playerPoolInfo != null) {
                        if (playerPoolInfo.getPlayer().getID().equals(playerID)) {
                            teamSeededPlayers.add(playerPoolInfo);
                            break;
                        }
                    }
                }
            } else {
                teamSeededPlayers.add(null);
            }
        }



// Create a map of player and team
// Create a score for the draw
// Swap players to reduce the score

        Map<Integer, Integer> poolToPositionMap = new HashMap<Integer, Integer>();

        for (int positionNo = 0; positionNo < numberOfPlayerPositions; positionNo++) {
            int poolLocation = positionToPoolMap.get(positionNo + 1) - 1;
            poolToPositionMap.put(poolLocation, positionNo);
        }
        Map<Integer, Integer> positionToSeedMap = new HashMap<Integer, Integer>();

        for (int seedNo = 0; seedNo < numberOfPlayerPositions; seedNo++) {
            int poolLocation = seedToPoolMap.get(seedNo);
//            int poolLocation = seedNo;
            int playerLocation = poolToPositionMap.get(poolLocation);
            positionToSeedMap.put(playerLocation, seedNo);
        }

        for (int playerLocation = 0; playerLocation < numberOfPlayerPositions; playerLocation++) {
            PlayerPoolInfo playerPoolInfo = teamSeededPlayers.get(positionToSeedMap.get(playerLocation));
            positionedPlayers.add(playerLocation, playerPoolInfo);
        }
        return positionedPlayers;
    }

    /* Parser instance stuff */
    private PoolDraw(TransactionalDatabase database, int poolID, Map<Integer, Integer> seeds) {
        pool = database.get(Pool.class, poolID);
        playerPoolInfoList = PoolPlayerSequencer.getPlayerSequence(database, poolID);
        fights = database.findAll(Fight.class, FightDAO.FOR_POOL, pool.getID());
        
        this.seeds = new HashMap<>();
        // For each player in division, copy seed into new seeds map
        for (PlayerPoolInfo playerPoolInfo : playerPoolInfoList) {
            if(playerPoolInfo != null) {
                int playerID = playerPoolInfo.getPlayer().getID();
                if(seeds.containsKey(playerID)) {
                    this.seeds.put(playerID, seeds.get(playerID));
                }
            }
        }

        CompetitionInfo ci = database.get(CompetitionInfo.class, null);
        configurationFile = ConfigurationFile.getConfiguration(ci.getDrawConfiguration());
        if (configurationFile == null) {
            log.error("Unable to load draw configuration.");
        }
    }

    private PoolDraw(TransactionalDatabase database, int poolID) {
        pool = database.get(Pool.class, poolID);
        playerPoolInfoList = PoolPlayerSequencer.getPlayerSequence(database, poolID);
        fights = database.findAll(Fight.class, FightDAO.FOR_POOL, pool.getID());

        CompetitionInfo ci = database.get(CompetitionInfo.class, null);
        configurationFile = ConfigurationFile.getConfiguration(ci.getDrawConfiguration());
        if (configurationFile == null) {
//            System.out.println("Unable to load draw configuration.");
            log.error("Unable to load draw configuration.");
        }
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
