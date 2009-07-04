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
import java.util.List;
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

    public DrawHTMLGenerator(Database database, int poolID, boolean showResults, boolean fullDocument) {
        this.database = database;
        this.poolID = poolID;
        this.showResults = showResults;
        this.fullDocument = fullDocument;
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
                  p.getVisibleID() + " " +
                  p.getLastName() + ", " +
                  p.getFirstName().charAt(0));
        }
        while(i++ < 32) {
            c.put("player" + i, "BYE");
        }

        c.put("p", ps);
        c.put("pd", pds);
        c.put("division", pool.getDescription());

        if(showResults) {
            PlayerCodeParser parser = PlayerCodeParser.getInstance(database, poolID);

            for(String code : new String[] {
                "LW5", "LW6", "LW9", "LW10", "LW11", "LW12",
                "L5", "L6", "L9", "L10", "L11", "L12", "L17", "L18",
                "W1", "W2", "W3", "W4", "W5", "W6", "W7", "W8", "W9", "W10",
                "W11", "W12", "W13", "W14", "W15", "W16", "W17", "W18", "W19", "W20",
                "W21", "W22", "W23"
            }) {
                FightPlayer p = parser.parseCode(code);
                switch(p.type) {
                    case NORMAL:
                        c.put(code,
                              p.player.getVisibleID() + " " +
                              p.player.getLastName() + ", " +
                              p.player.getFirstName().charAt(0));
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

            List<Fight> fights = database.findAll(Fight.class, FightDAO.FOR_POOL, poolID);
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
