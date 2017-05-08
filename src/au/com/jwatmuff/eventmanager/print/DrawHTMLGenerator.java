/*
 * EventManager
 * Copyright (c) 2008-2017 James Watmuff & Leonard Hall
 *
 * This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package au.com.jwatmuff.eventmanager.print;

import au.com.jwatmuff.eventmanager.db.FightDAO;
import au.com.jwatmuff.eventmanager.db.ResultDAO;
import au.com.jwatmuff.eventmanager.db.SessionDAO;
import au.com.jwatmuff.eventmanager.db.SessionFightDAO;
import au.com.jwatmuff.eventmanager.model.config.ConfigurationFile;
import au.com.jwatmuff.eventmanager.model.info.PlayerPoolInfo;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser.CodeInfo;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser.CodeType;
import au.com.jwatmuff.eventmanager.model.misc.PlayerCodeParser.FightPlayer;
import au.com.jwatmuff.eventmanager.model.misc.PoolPlayerSequencer;
import au.com.jwatmuff.eventmanager.model.misc.SessionFightSequencer;
import au.com.jwatmuff.eventmanager.model.misc.SessionFightSequencer.FightMatInfo;
import au.com.jwatmuff.eventmanager.model.vo.CompetitionInfo;
import au.com.jwatmuff.eventmanager.model.vo.Fight;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.model.vo.Pool.Place;
import au.com.jwatmuff.eventmanager.model.vo.Result;
import au.com.jwatmuff.eventmanager.model.vo.Session;
import au.com.jwatmuff.eventmanager.model.vo.SessionFight;
import au.com.jwatmuff.genericdb.Database;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
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

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

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

//Add page break at the top of the page if this isn't the first page

        if(firstPage) c.put("first", "true");

//Print the competition name and division
        CompetitionInfo compInfo = database.get(CompetitionInfo.class, null);
        ConfigurationFile configurationFile = ConfigurationFile.getConfiguration(compInfo.getDrawConfiguration());
        c.put("competitionName", compInfo.getName());
        if(StringUtils.isEmpty(compInfo.getLocation()))
            c.put("competitionInfo",
                    DATE_FORMAT.format(compInfo.getStartDate()) + " to " +
                    DATE_FORMAT.format(compInfo.getEndDate()));
        else
            c.put("competitionInfo",
                    "Location: " + compInfo.getLocation() + ", " +
                    DATE_FORMAT.format(compInfo.getStartDate()) + " to " +
                    DATE_FORMAT.format(compInfo.getEndDate()));
        
        Pool pool = database.get(Pool.class, poolID);

        
        assert(pool != null && pool.getLockedStatus() != Pool.LockedStatus.UNLOCKED);

//Print the player names and information

        List<Player> playerList = new ArrayList<Player>();
        List<PlayerPoolInfo> playerPoolInfoList = PoolPlayerSequencer.getPlayerSequence(database, poolID);
        int i = 0;
        for(PlayerPoolInfo playerPoolInfo : playerPoolInfoList) {
            i++;
            
            if(playerPoolInfo == null) {
                c.put("player" + i, "BYE");
                continue;
            }

            Player player = playerPoolInfo.getPlayer();
            playerList.add(player);
            if(playerPoolInfo.isWithdrawn())
                c.put("player" + i, player.getLastName() + ", " + player.getFirstName() + " (FG)" );
            else
                c.put("player" + i, player.getLastName() + ", " + player.getFirstName() );
            
            if (player.getTeam() != null) {
                c.put("playerRegion" + i, player.getTeam() + " (" + player.getShortGrade() + ")" );
            } else {
                c.put("playerRegion" + i, " (" + player.getShortGrade() + ")" );
            }
        }
        while(i++ < 64) { //TODO: this should not be an arbitrary limit
            c.put("player" + i, "BYE");
        }
            
            PlayerCodeParser parser = PlayerCodeParser.getInstance(database, poolID);

            Set<String> codes = new HashSet<String>();

//Print the draw fight number or the mat fight number

        // Work out if this division been assigned to a locked session
        boolean divisionAssignedToSession = false;
        for(Session session : database.findAll(Session.class, SessionDAO.FOR_POOL, poolID)) {
            if(session.getLockedStatus() != Session.LockedStatus.UNLOCKED) {
                divisionAssignedToSession = true;
                break;
            }
        }

        List<Fight> fights = database.findAll(Fight.class, FightDAO.FOR_POOL, poolID);
            /* add all codes used in any fights */
            for(Fight fight : fights) {
                codes.addAll(Arrays.asList(fight.getPlayerCodes()));
            }
//Look for round robins and print them

            for(String code : codes) {
                CodeInfo codeInfo = PlayerCodeParser.getCodeInfo(code);
                if(codeInfo.type == CodeType.ROUNDROBIN){
                    c = parser.roundRobinContext(codeInfo.code, c, showResults);
                }
            }
            
        /* add fight numbers */
        if(showResults && divisionAssignedToSession) {
            List<String> matNames = new ArrayList<String>();
            for(i = 1; i <= fights.size(); i++) {
                try {
                    Fight fight = fights.get(i - 1);
                    SessionFight sessionFight = database.find(SessionFight.class, SessionFightDAO.FOR_FIGHT, fight.getID());
                    FightMatInfo fightMatInfo = SessionFightSequencer.getFightMatInfo(database, sessionFight);
                    c.put("fightNumber" + i, fightMatInfo.fightNumber);
                    if(!matNames.contains(fightMatInfo.matName))
                        matNames.add(fightMatInfo.matName);
//                    c.put("fightMat" + i, fightMatInfo.matName);
                } catch(Exception e) {
                    // This will happen if the fight has not been added to a session (i.e. no SessionFight exists).
                    // Because we know the division has been added to a session, this means it must be a bye fight.
                    c.put("fightNumber" + i, "--");
//                    c.put("fightMat" + i, "");
                }
            }
// Print the competition info
            c.put("division", pool.getDescription() + ". " + matNames);
        } else {
            for(i = 1; i <= fights.size(); i++) {
                c.put("fightNumber" + i, i);
                c.put("fightMat" + i, "");
            }
// Print the division
            c.put("division", pool.getDescription());
        }

//Add and convert the draw codes to player names. Adds WX and LX codes for individual fight results.

        if(showResults) {
            /* add winner and loser codes for all fights */
            for(i = 1; i <= fights.size(); i++) {
                codes.add("W" + i);
                codes.add("L" + i);
            }

            for(String code : codes) {
                FightPlayer fightPlayer = parser.parseCode(code);
                switch(fightPlayer.type) {
                    case NORMAL:
                        c.put(PlayerCodeParser.getORCodes(code)[0],
                              fightPlayer.player.getLastName() + ", " + fightPlayer.player.getFirstName().charAt(0));
                        break;
                    case ERROR:
                        c.put(PlayerCodeParser.getORCodes(code)[0], "--"+code); // mark error with --
                        break;
                    case UNDECIDED:
                        c.put(PlayerCodeParser.getORCodes(code)[0], "          ");
                        break;
                    case EMPTY:
                        c.put(PlayerCodeParser.getORCodes(code)[0], "          ");
                        break;
                    case BYE:
                        if(fightPlayer.playerPoolInfo != null && fightPlayer.playerPoolInfo.isWithdrawn() )
                            c.put(PlayerCodeParser.getORCodes(code)[0],
                                  fightPlayer.player.getLastName() + ", " + fightPlayer.player.getFirstName().charAt(0));
                        else
                            c.put(PlayerCodeParser.getORCodes(code)[0], "BYE");
                        break;
                    default:
                        c.put(PlayerCodeParser.getORCodes(code)[0], fightPlayer.type.toString());
                        break;
                }
            }

//Adds the place names and winning player names

            i = 0;
            List<Place> places = pool.getPlaces();
            for(Place place : places) {
//Add the place codes to list of codes
                codes.addAll(Arrays.asList(PlayerCodeParser.getORCodes(place.code)));
                FightPlayer fightPlayer = parser.parseCode(place.code);

                i++;
                switch(fightPlayer.type) {
                    case NORMAL:
                        if(fightPlayer.player != null) {
                            if (fightPlayer.player.getTeam() != null) {
                                c.put("place" + i, place.name + ": " + fightPlayer.player.getLastName() + ", " + fightPlayer.player.getFirstName() + " -- " + fightPlayer.player.getTeam());
                            } else {
                                c.put("place" + i, place.name + ": " + fightPlayer.player.getLastName() + ", " + fightPlayer.player.getFirstName());
                            }
                        } else {
                            c.put("place" + i, place.name + ": Error" + "--" + fightPlayer.code);
                        }
                        break;
                    case ERROR:
                        c.put("place" + i, place.name + "--" + fightPlayer.code); // mark error with --
                        break;
                    case UNDECIDED:
                        c.put("place" + i, place.name + ": UNDECIDED");
                        break;
                    case BYE:
                        if(fightPlayer.playerPoolInfo != null && fightPlayer.playerPoolInfo.isWithdrawn() )
                            if (fightPlayer.player.getTeam() != null) {
                                c.put("place" + i, place.name + ": " + fightPlayer.player.getLastName() + ", " + fightPlayer.player.getFirstName() + " -- " + fightPlayer.player.getTeam());
                            } else {
                                c.put("place" + i, place.name + ": " + fightPlayer.player.getLastName() + ", " + fightPlayer.player.getFirstName());
                            }
                        else
                            c.put("place" + i, place.name + ":      ");
                        break;
                    default:
                        c.put("place" + i, place.name + "--" + fightPlayer.code); // mark error with --
                        break;
                }
            }

//Adds the score and points for each fight

            i = 0;
            for(Fight fight : fights) {
                i++;
                List<Result> results = database.findAll(Result.class, ResultDAO.FOR_FIGHT, fight.getID());
                if(!results.isEmpty()) {
                    c.put("result_" + i, results.get(0).getScores()[0].displayString() + " / " + results.get(0).getScores()[1].displayString() + "  " + results.get(0).getDurationString());
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
