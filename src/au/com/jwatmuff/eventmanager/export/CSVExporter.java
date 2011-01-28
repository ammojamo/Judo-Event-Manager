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
import au.com.jwatmuff.eventmanager.model.vo.PlayerDetails;
import au.com.jwatmuff.eventmanager.model.vo.Result;
import au.com.jwatmuff.genericdb.Database;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import javax.swing.JTable;
import javax.swing.table.TableModel;
import org.apache.commons.lang.ArrayUtils;

/**
 *
 * @author James
 */
public class CSVExporter {
    private static final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");

    /** Creates a new instance of TextExporter */
    private CSVExporter() {
    }

    public static void generateFromTable(JTable t, OutputStream out) {
        generateFromTable(t.getModel(), out);
    }

    public static void generateFromTable(TableModel tm, OutputStream out) {
        int rows = tm.getRowCount();
        int cols = tm.getColumnCount();
        Object[][] table = new Object[rows+1][cols];
        for(int j = 0; j < cols; j++)
            table[0][j] = tm.getColumnName(j);
        for(int i = 0; i < rows; i++)
            for(int j = 0; j < cols; j++)
                table[i+1][j] = tm.getValueAt(i, j);

        generateFromArray(table, out);
    }

    public static void generateFromArray(Object[][] table, OutputStream os) {
        PrintStream ps = new PrintStream(os);
        for(Object[] row : table)
            outputRow(row, ps);
    }

    public static void generatePlayerList(Database database, OutputStream out) {
        Collection<Player> players = database.findAll(Player.class, PlayerDAO.ALL);
        PrintStream ps = new PrintStream(out);
        
        Object[] columns = new Object[] {
            "id",
            "First Name",
            "Last Name",
            "Sex",
            "Grade",
            "DOB",
            "Team",
            "Home Number",
            "Work Number",
            "Mobile Number",
            "Street",
            "City",
            "Postcode",
            "State",
            "Email",
            "Emergency Name",
            "Emergency Phone",
            "Emergency Mobile",
            "Medical Conditions",
            "Medical Info",
            "Injury Info"};

        outputRow(columns, ps);

        for(Player player : players) {
            Object[] fields1 = new Object[] {
                player.getVisibleID(),
                player.getFirstName(),
                player.getLastName(),
                player.getGender(),
                player.getGrade(),
                dateFormat.format(player.getDob()),
                player.getTeam()
            };
            
            PlayerDetails details = database.get(PlayerDetails.class, player.getDetailsID());
            Object[] fields2;
            if(details != null) {
                fields2 = new Object[]{
                            details.getHomeNumber(),
                            details.getWorkNumber(),
                            details.getMobileNumber(),
                            details.getStreet(),
                            details.getCity(),
                            details.getPostcode(),
                            details.getState(),
                            details.getEmail(),
                            details.getEmergencyName(),
                            details.getEmergencyPhone(),
                            details.getEmergencyMobile(),
                            details.getMedicalConditions(),
                            details.getMedicalInfo(),
                            details.getInjuryInfo()};
            } else {
                fields2 = new Object[14];
                Arrays.fill(fields2, null);
            }

            outputRow(ArrayUtils.addAll(fields1, fields2), ps);
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
                ps.print(ri.getResult().getScores()[0] + ",");
                ps.print(ri.getResult().getScores()[1]);
                
                ps.println();
            } catch (DatabaseStateException e) {
                // do nothing
            }
        }
    }

    private static void outputRow(Object[] fields, PrintStream out) {
        boolean first = true;
        for(Object field : fields) {
            if(!first) out.print(",");
            if(field != null) {
                if(field instanceof Number) {
                    out.print(field);
                } else {
                    out.print("\"" + field + "\"");
                }
            }
            first = false;
        }
        out.println();
    }
}
