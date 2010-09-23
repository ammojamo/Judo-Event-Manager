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
import au.com.jwatmuff.eventmanager.model.vo.Result;
import au.com.jwatmuff.genericdb.Database;
import java.util.ArrayList;
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
        c.put("competitionName", database.get(CompetitionInfo.class, null).getName());
        
        Pool pool = database.get(Pool.class, poolID);
        
        assert(pool != null && pool.getLockedStatus() != Pool.LockedStatus.UNLOCKED);

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
            c.put("player" + i, p.getLastName() + ", " + p.getFirstName().charAt(0) );
            wins.put(p.getID(),0);
            points.put(p.getID(),0);
            if (pd.getClub() == null) {
                c.put("player" + i + "Details", "(" + p.getGrade() + ")" );
            } else {
                c.put("player" + i + "Details", "(" + p.getGrade() + ", " + pd.getClub() + ")" );
            }
        }
        while(i++ < 64) { //TODO: this should not be an arbitrary limit
            c.put("player" + i, "BYE");
        }

        c.put("p", ps);
        c.put("pd", pds);
        c.put("division", pool.getDescription());

        if(firstPage) c.put("first", "true");

        if(showResults) {
            PlayerCodeParser parser = PlayerCodeParser.getInstance(database, poolID);

            List<Fight> fights = database.findAll(Fight.class, FightDAO.FOR_POOL, poolID);

            Set<String> codes = new HashSet<String>();
            /* add all codes used in any fights */
            for(Fight fight : fights) {
                for(String code : fight.getPlayerCodes())
                    codes.add(code);
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
                        c.put(code, "--"); // mark error with --
                        break;
                    case UNDECIDED:
                        c.put(code, code);
                        break;
                    default:
                        c.put(code, p.type.toString());
                        break;
                }
            }

            i = 0;
            for(Fight f : fights) {
                i++;
                List<Result> results = database.findAll(Result.class, ResultDAO.FOR_FIGHT, f.getID());
                if(!results.isEmpty()) {
                    int[] scores = results.get(0).getPlayerScores();
                    int[] ids = results.get(0).getPlayerIDs();
                    c.put("fight" + i + "points1", scores[0]);
                    c.put("fight" + i + "points2", scores[1]);
                    if(scores[0] > scores[1]) {
                        wins.put(ids[0],wins.get(ids[0])+1);
                        points.put(ids[0],points.get(ids[0])+scores[0]);
                    } else {
                        wins.put(ids[1],wins.get(ids[1])+1);
                        points.put(ids[1],points.get(ids[1])+scores[1]);
                    }
                    switch(scores[0]) {
                        case 1:
                            c.put("fight" + i + "score1", "D");
                            break;
                        case 5:
                            c.put("fight" + i + "score1", "Y");
                            break;
                        case 7:
                            c.put("fight" + i + "score1", "W");
                            break;
                        case 10:
                            c.put("fight" + i + "score1", "I");
                            break;
                        default:
                            c.put("fight" + i + "score1", " ");
                            break;
                    }
                    switch(scores[1]) {
                        case 1:
                            c.put("fight" + i + "score2", "D");
                            break;
                        case 5:
                            c.put("fight" + i + "score2", "Y");
                            break;
                        case 7:
                            c.put("fight" + i + "score2", "W");
                            break;
                        case 10:
                            c.put("fight" + i + "score2", "I");
                            break;
                        default:
                            c.put("fight" + i + "score2", " ");
                            break;
                    }
                }
            }
            i = 0;
            for(PlayerPoolInfo player : pPS) {
                i++;
                Player p = player.getPlayer();
                c.put("player" + i + "wins", wins.get(p.getID()) );
                c.put("player" + i + "points", points.get(p.getID()) );
            }
        }
        c.put("fullDocument", fullDocument);
    }

    @Override
    public String getTemplateName() {
        return database.get(Pool.class, poolID).getTemplateName() + ".html";
    }
}
