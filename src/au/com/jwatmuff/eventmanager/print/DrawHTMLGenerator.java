/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.print;

import au.com.jwatmuff.eventmanager.db.FightDAO;
import au.com.jwatmuff.eventmanager.db.ResultDAO;
import au.com.jwatmuff.eventmanager.model.info.PlayerPoolInfo;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser.FightPlayer;
import au.com.jwatmuff.eventmanager.model.misc.PoolPlayerSequencer;
import au.com.jwatmuff.eventmanager.model.vo.CompetitionInfo;
import au.com.jwatmuff.eventmanager.model.vo.Fight;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.PlayerDetails;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.model.vo.Pool.Place;
import au.com.jwatmuff.eventmanager.model.vo.Result;
import au.com.jwatmuff.genericdb.Database;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import org.apache.velocity.context.Context;

/**
 *
 * @author James
 */
public class DrawHTMLGenerator extends VelocityHTMLGenerator {
    private Database database;
    private int poolID;
    private boolean showResults;
    private boolean fullDocument;
    private boolean firstPage;

    public DrawHTMLGenerator(Database database, int poolID, boolean showResults, boolean fullDocument, boolean firstPage) {
        this.database = database;
        this.poolID = poolID;
        this.showResults = showResults;
        this.fullDocument = fullDocument;
        this.firstPage = firstPage;
    }

    public DrawHTMLGenerator(Database database, int poolID, boolean showResults, boolean fullDocument) {
        this(database, poolID, showResults, fullDocument, false);
    }

    public DrawHTMLGenerator(Database database, int poolID, boolean showResults) {
        this(database, poolID, showResults, true);
    }

    public DrawHTMLGenerator(Database database, int poolID) {
        this(database, poolID, false);
    }

    @Override
    public void populateContext(Context c) {
        Map<Integer,Integer> wins = new HashMap<Integer,Integer>();
        Map<Integer,Integer> points = new HashMap<Integer,Integer>();

//Add page break at the top of the page if this isn't the first page

        if(firstPage) c.put("first", "true");

//Print the competition information and division

        c.put("competitionName", database.get(CompetitionInfo.class, null).getName());
        c.put("competitionInfo",
                "Location: " + database.get(CompetitionInfo.class, null).getLocation() + ", " +
                database.get(CompetitionInfo.class, null).getStartDate() + " to " +
                database.get(CompetitionInfo.class, null).getEndDate());
        
        Pool pool = database.get(Pool.class, poolID);

        c.put("division", pool.getDescription());
        
        assert(pool != null && pool.getLockedStatus() != Pool.LockedStatus.UNLOCKED);

//Print the player names and information

        List<Player> ps = new ArrayList<Player>();
        List<PlayerDetails>pds = new ArrayList<PlayerDetails>();
        List<PlayerPoolInfo> pPS = PoolPlayerSequencer.getPlayerSequence(database, poolID);
        int i = 0;
        for(PlayerPoolInfo player : pPS) {
            i++;
            if(player == null) {
                c.put("player" + i, "BYE");
                continue;
            }
            Player p = player.getPlayer();
            PlayerDetails pd = database.get(PlayerDetails.class, p.getDetailsID());
            ps.add(p);
            pds.add(pd);
//            c.put("player" + i, p.getLastName() + ", " + p.getFirstName().charAt(0) );
            c.put("player" + i, p.getLastName() + ", " + p.getFirstName() );
            wins.put(p.getID(),0);
            points.put(p.getID(),0);
            if (pd.getClub() != null) {
                c.put("playerRegion" + i, pd.getClub() + " (" + p.getShortGrade() + ")" );
            } else {
                c.put("playerRegion" + i, " (" + p.getShortGrade() + ")" );
            }
        }
        while(i++ < 64) { //TODO: this should not be an arbitrary limit
            c.put("player" + i, "BYE");
        }

//        c.put("p", ps);
//        c.put("pd", pds);

//Print the draw fight number or the mat fight number

        List<Fight> fights = database.findAll(Fight.class, FightDAO.FOR_POOL, poolID);
        /* add fight numbers */
        for(i = 1; i <= fights.size(); i++) {
            c.put("fightNumber" + i, i);
        }

//Add and convert the draw codes to player names. Adds WX and LX codes for individual fight results.

        if(showResults) {
            PlayerCodeParser parser = PlayerCodeParser.getInstance(database, poolID);

            Set<String> codes = new HashSet<String>();
            /* add all codes used in any fights */
            for(Fight fight : fights) {
                codes.addAll(Arrays.asList(fight.getPlayerCodes()));
            }
            /* add winner and loser codes for all fights */
            for(i = 1; i <= fights.size(); i++) {
                codes.add("W" + i);
                codes.add("L" + i);
            }

            for(String code : codes) {
                FightPlayer p = parser.parseCode(code);

                switch(p.type) {
                    case NORMAL:
                        c.put(code,
                              p.player.getLastName() + ", " + p.player.getFirstName().charAt(0));
                        break;
                    case ERROR:
                        c.put(code, "--"+code); // mark error with --
                        break;
                    case UNDECIDED:
                        c.put(code, code);
                        break;
                    default:
                        c.put(code, p.type.toString());
                        break;
                }
            }

//Adds the score and points for each fight

            i = 0;
            for(Fight f : fights) {
                i++;
                List<Result> results = database.findAll(Result.class, ResultDAO.FOR_FIGHT, f.getID());
                if(!results.isEmpty()) {
                    int[] scores = results.get(0).getSimpleScores();
                    int[] ids = results.get(0).getPlayerIDs();

                    c.put("result_" + i, results.get(0).getScores()[0].displayString() + " / " + results.get(0).getScores()[1].displayString() + "  " + results.get(0).getDurationString());
                    

                    c.put("fight" + i + "points1", scores[0]);
                    c.put("fight" + i + "points2", scores[1]);
                    if(scores[0] > scores[1]) {
                        wins.put(ids[0],wins.get(ids[0])+1);
                        points.put(ids[0],points.get(ids[0])+scores[0]);
                    } else {
                        wins.put(ids[1],wins.get(ids[1])+1);
                        points.put(ids[1],points.get(ids[1])+scores[1]);
                    }

//                    List<PlayerScore> scores = new ArrayList<PlayerScore>();
//                    for(Integer playerID : playerScores.keySet()) {
//                        PlayerScore score = new PlayerScore();
//                        score.playerID = playerID;
//                        score.score = playerScores.get(playerID);
//                        scores.add(score);
//                    }

                }
            }

//Adds total wins and points

            i = 0;
            for(PlayerPoolInfo player : pPS) {
                i++;
                if(player!=null){
                    Player p = player.getPlayer();
                    c.put("player" + i + "wins", wins.get(p.getID()) );
                    c.put("player" + i + "points", points.get(p.getID()) );
                }
            }

//Adds the place names and winning player names

            i = 0;
            List<Place> places = pool.getPlaces();
            Map<Place,FightPlayer> placeFightPlayer = parser.parsePlaces(places);
            for(Place place : places) {
                i++;
                switch(placeFightPlayer.get(place).type) {
                    case NORMAL:
                        PlayerDetails pd = database.get(PlayerDetails.class, placeFightPlayer.get(place).player.getDetailsID());
                        if (pd.getClub() != null) {
                            c.put("place" + i, place.name + ": " + placeFightPlayer.get(place).player.getLastName() + ", " + placeFightPlayer.get(place).player.getFirstName() + " -- " + pd.getClub());
                        } else {
                            c.put("place" + i, place.name + ": " + placeFightPlayer.get(place).player.getLastName() + ", " + placeFightPlayer.get(place).player.getFirstName());
                        }
                        break;
                    case ERROR:
                        c.put("place" + i, "--"); // mark error with --
                        break;
                    case UNDECIDED:
//                            c.put("place" + i, place.name + ": ");
                        break;
                    default:
                        c.put("place" + i, place.name);
                        break;
                }
            }
        }
        c.put("fullDocument", fullDocument);
    }

    @Override
    public String getTemplateName() {
        return database.get(Pool.class, poolID).getTemplateName() + ".html";
    }
}
