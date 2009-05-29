/*
 * TextExporter.java
 *
 * Created on 26 August 2008, 05:07
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.export;

import au.com.jwatmuff.eventmanager.db.PlayerDAO;
import au.com.jwatmuff.eventmanager.db.ResultDAO;
import au.com.jwatmuff.eventmanager.model.cache.ResultInfoCache;
import au.com.jwatmuff.eventmanager.model.info.ResultInfo;
import au.com.jwatmuff.eventmanager.model.misc.DatabaseStateException;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.Result;
import au.com.jwatmuff.genericdb.Database;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collection;

/**
 *
 * @author James
 */
public class CSVExporter {
    
    /** Creates a new instance of TextExporter */
    private CSVExporter() {
    }
    
    public static void generatePlayerList(Database database, OutputStream out) {
        Collection<Player> players = database.findAll(Player.class, PlayerDAO.ALL);
        PrintStream ps = new PrintStream(out);
        
        for(Player player : players) {
            ps.print("\"" + player.getLastName().toUpperCase() + "\",");
            ps.print("\"" + player.getFirstName() + "\",");
            ps.print(player.getDob() + ",");
            ps.print("\"" + player.getGender() + "\",");
            ps.print("\"" + player.getGrade() + "\",");
//            ps.print("\"" + player.getMedicalConditions() + "\",");
//            ps.print("\"" + player.getMedicalInfo() + "\",");
            ps.println();
        }
    }
    
    public static void generateResults(Database database, ResultInfoCache cache, OutputStream out) {
        PrintStream ps = new PrintStream(out);
        
        for(Result result : database.findAll(Result.class, ResultDAO.ALL)) {
            try {
                ResultInfo ri = cache.getResultInfo(result.getID());
                ps.print("\"" + ri.getMatName() + "\",");
                ps.print(ri.getMatFightNumber() + ",");
                ps.print("\"" + ri.getPlayerName()[0] + "\",");
                ps.print("\"" + ri.getPlayerName()[1] + "\",");
                ps.print(ri.getResult().getPlayerScores()[0] + ",");
                ps.print(ri.getResult().getPlayerScores()[1]);
                
                ps.println();
            } catch (DatabaseStateException e) {
                // do nothing
            }
        }
    }
}
