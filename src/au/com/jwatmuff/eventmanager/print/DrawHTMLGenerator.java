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
        c.put("competitionName", database.get(CompetitionInfo.class, null).getName());
        
        Pool pool = database.get(Pool.class, poolID);
        
        assert(pool != null && pool.getLockedStatus() != Pool.LockedStatus.UNLOCKED);

        List<Player> ps = new ArrayList<Player>();
        List<PlayerDetails>pds = new ArrayList<PlayerDetails>();
        int i = 0;
        for(PlayerPoolInfo player : PoolPlayerSequencer.getPlayerSequence(database, poolID)) {
            i++;
            Player p = player.getPlayer();
            PlayerDetails pd = database.get(PlayerDetails.class, p.getDetailsID());
            ps.add(p);
            pds.add(pd);
            c.put("player" + i,
                  p.getLastName() + ", " + p.getFirstName().charAt(0) + " (" + p.getGrade() + ")" );
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
                    c.put("fight" + i + "score1", scores[0]);
                    c.put("fight" + i + "score2", scores[1]);
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
