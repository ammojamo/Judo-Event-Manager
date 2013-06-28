/*
 * CSVImporter.java
 *
 * Created on 31 July 2008, 17:11
  */

package au.com.jwatmuff.eventmanager.model.misc;

import au.com.bytecode.opencsv.CSVReader;
import au.com.jwatmuff.eventmanager.db.FightDAO;
import au.com.jwatmuff.eventmanager.db.PlayerDAO;
import au.com.jwatmuff.eventmanager.db.PoolDAO;
import au.com.jwatmuff.eventmanager.model.vo.Fight;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.Player.Grade;
import au.com.jwatmuff.eventmanager.model.vo.PlayerDetails;
import au.com.jwatmuff.eventmanager.model.vo.PlayerPool;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.model.vo.Pool.Place;
import au.com.jwatmuff.eventmanager.util.CSVBeanReader;
import au.com.jwatmuff.eventmanager.util.CSVMapReader;
import au.com.jwatmuff.genericdb.transaction.Transaction;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 * Class containing static methods responsible for processing parsed input from
 * a CSV file, and creating/updating entries in the database accordingly.
 *
 * @author James
 */
public class CSVImporter {
    private static Logger log = Logger.getLogger(CSVImporter.class);
    
    private static final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");

    /** Creates a new instance of CSVImporter */
    private CSVImporter() {
    }
    
    public static class TooFewPlayersException extends Exception {
    }

    private static List<Place> importDrawPlaces(final File csvFile) throws IOException {
        List<Place> places = new ArrayList<Place>();
        if(csvFile.exists()) {
            CSVReader reader = new CSVReader(new FileReader(csvFile));

            Map<String, String> columnMapping = new HashMap<String, String>();
            columnMapping.put("Place", "name");
            columnMapping.put("Code", "code");

            CSVMapReader beanReader = new CSVMapReader(reader, columnMapping);

            for(Map<String, String> entry : beanReader.readRows()) {
                Place place = new Place();
                place.code = entry.get("code");
                place.name = entry.get("name");
                places.add(place);
            }
        }
        return places;
    }

    private static Map<Integer, Integer> importDrawPools(final File csvFile) throws IOException {
        Map<Integer, Integer> poolEntries = new HashMap<Integer, Integer>();

        if(csvFile.exists()) {
            CSVReader reader = new CSVReader(new FileReader(csvFile));

            Map<String, String> columnMapping = new HashMap<String, String>();
            columnMapping.put("Pool", "pool");
            columnMapping.put("Player", "player");

            CSVMapReader beanReader = new CSVMapReader(reader, columnMapping);

            for(Map<String,String> entry : beanReader.readRows()) {
                poolEntries.put(
                        Integer.valueOf(entry.get("player")),
                        Integer.valueOf(entry.get("pool")));
            }
        }
        return poolEntries;
    }

    public static int importFightDraw(final File csvFile, final TransactionalDatabase database, final Pool pool, int minPlayers) throws IOException, DatabaseStateException, TooFewPlayersException {
        CSVReader reader = new CSVReader(new FileReader(csvFile));
        
        Map<String, String> columnMapping = new HashMap<String, String>();
        columnMapping.put("Fight", "position");
        columnMapping.put("Player 1", "code1");
        columnMapping.put("Player 2", "code2");
        
        CSVMapReader mapReader = new CSVMapReader(reader, columnMapping);
        List<Map<String, String>> fights = mapReader.readRows();

        File placesFile = new File(csvFile.getAbsolutePath().replace(".csv", ".places.csv"));
        final List<Place> places = importDrawPlaces(placesFile);

        File poolsFile = new File(csvFile.getAbsolutePath().replace(".csv", ".pools.csv"));
        final Map<Integer, Integer> drawPools = importDrawPools(poolsFile);
        
        if(pool.getLockedStatus() == Pool.LockedStatus.UNLOCKED)
            throw new DatabaseStateException("Cannot generate fights for unlocked pool");
        if(pool.getLockedStatus() == Pool.LockedStatus.FIGHTS_LOCKED)
            throw new DatabaseStateException("Fights are already locked for this pool");
        
        // remove any previously defined fights for this pool
        final Collection<Fight> oldFights = database.findAll(Fight.class, FightDAO.FOR_POOL, pool.getID());
        if(oldFights.size() > 0) {
            database.perform(new Transaction() {
                @Override
                public void perform() {
                    for(Fight fight : oldFights)
                        database.delete(fight);
                }
            });
        }
        
        final Collection<Fight> newFights = new ArrayList<Fight>();
        
        int failed = 0;
        int succeeded = 0;
        
        int maxPlayer = 0;
        for(Map<String, String> fightInfo : fights) {
            try {
                Fight fight = new Fight();
                fight.setPosition(Integer.parseInt(fightInfo.get("position")));
                fight.setPlayerCode(0, fightInfo.get("code1"));
                fight.setPlayerCode(1, fightInfo.get("code2"));
                fight.setPoolID(pool.getID());
                newFights.add(fight);

                for(String code : fight.getPlayerCodes()) {
                    if(PlayerCodeParser.getPrefix(code).equals("P")) {
                        int number = PlayerCodeParser.getNumber(code);
                        if(number > maxPlayer) maxPlayer = number;
                    }
                }
                succeeded++;
            } catch(Exception e) {
                failed++;
                log.error("Error while adding fight from CSV file to database", e);
            }
        }
        
        if(maxPlayer >= minPlayers) {
            String fname = csvFile.getName();
            final String templateName = fname.substring(0, fname.lastIndexOf("."));

            database.perform(new Transaction() {
                @Override
                public void perform() {
                    for(Fight fight : newFights) {
                        database.add(fight);
                    }
                    pool.setTemplateName(templateName);
                    pool.setPlaces(places);
                    pool.setDrawPools(drawPools);
                    database.update(pool);
                }
            });
            return succeeded;
        } else {
            throw new TooFewPlayersException();
        }
    }
    
    private static int parseInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch(Exception e) {
            return 0;
        }
    }
    
    public static int importPools(File csvFile, final TransactionalDatabase database) throws IOException {
        CSVReader reader = new CSVReader(new FileReader(csvFile));
        
        Map<String, String> columnMapping = new HashMap<String, String>();
        columnMapping.put("Division", "description");
        columnMapping.put("Short Name", "shortName");
        columnMapping.put("Max Weight", "maximumWeight");
        columnMapping.put("Min Weight", "minimumWeight");
        columnMapping.put("Max Grade", "maximumGrade");
        columnMapping.put("Min Grade", "minimumGrade");
        columnMapping.put("Max Age", "maximumAge");
        columnMapping.put("Min Age", "minimumAge");
        columnMapping.put("Match Time", "matchTime");
        columnMapping.put("Golden Score Time", "goldenScoreTime");
        columnMapping.put("Minimum Break Time", "minimumBreakTime");
        columnMapping.put("Sex", "gender");
        Collection<String> requiredHeadings = Arrays.asList("Division", "Max Weight", "Min Weight", "Max Grade", "Min Grade", "Max Age", "Min Age", "Sex");

        CSVBeanReader<Pool> beanReader = new CSVBeanReader<Pool>(reader, columnMapping, Pool.class, requiredHeadings);

        final List<Pool> pools = beanReader.readBeans();

        database.perform(new Transaction() {
            @Override
            public void perform() {
                for(Pool pool : pools) {
                    try {
                        
                        database.add(pool);
                    } catch(Exception e) {
                        log.error("Error while adding pool from CSV file to database", e);
                    }
                }
            }
        });

        return pools.size();
    }
        
    public static int importPlayers(File csvFile, final TransactionalDatabase database) throws IOException {
        CSVReader reader = new CSVReader(new FileReader(csvFile));
            
        Map<String,String> columnMapping = new HashMap<String, String>();
        columnMapping.put("id", "ID");
        columnMapping.put("First Name", "firstName");
        columnMapping.put("Last Name", "lastName");
        columnMapping.put("Sex", "gender");
        columnMapping.put("Gender", "gender");
        columnMapping.put("Grade", "grade");
        columnMapping.put("DOB", "dob");
        columnMapping.put("Division", "division");
        columnMapping.put("Seed", "seed");
        columnMapping.put("Club", "club");
        columnMapping.put("Team", "club");

        columnMapping.put("Home Number", "homeNumber");
        columnMapping.put("Work Number", "workNumber");
        columnMapping.put("Mobile Number", "mobileNumber");

        columnMapping.put("Street", "street");
        columnMapping.put("City", "city");
        columnMapping.put("Postcode", "postcode");
        columnMapping.put("State", "state");
        columnMapping.put("Email", "email");

        columnMapping.put("Emergency Name", "emergencyName");
        columnMapping.put("Emergency Phone", "emergencyPhone");
        columnMapping.put("Emergency Mobile", "emergencyMobile");

        columnMapping.put("Medical Conditions", "medicalConditions");
        columnMapping.put("Medical Info", "medicalInfo");
        columnMapping.put("Injury Info", "injuryInfo");

        Collection<String> requiredHeadings = Arrays.asList("id", "First Name", "Last Name");
        //columnMapping.put("Weight", "weight");
        
        /* for some unknown reason, the bean reader doesn't play nice with DOB field */
        //CSVBeanReader<Player> beanReader = new CSVBeanReader<Player>(reader, columnMapping, Player.class);
            
        CSVMapReader mapReader = new CSVMapReader(reader, columnMapping, requiredHeadings);
        List<Map<String,String>> rows = mapReader.readRows();

        final List<Player> players = new ArrayList<Player>();

        final List<PlayerPool> pps = new ArrayList<PlayerPool>();

        final List<PlayerDetails> pds = new ArrayList<PlayerDetails>();

        int rowNumber = 0;
        for(Map<String,String> row : rows) {
            rowNumber++;

            /* detect and discard empty rows */
            boolean empty = true;
            for(String value : row.values()) {
                if(value != null && !value.isEmpty()) {
                    empty = false;
                    break;
                }
            }
            if(empty) {
                log.debug("Ignoring empty row in CSV file");
                continue;
            }

            /* attempt to look up ID in database. if that fails, create a new
             * player.
             */
            Player p = null;
            String id = row.get("ID");
            if(!id.matches("[a-zA-Z0-9]*"))
                throw new RuntimeException("Player IDs may only consist of letters and numbers with no spaces (invalid: " + id + ")");
            try {
                p = database.find(Player.class, PlayerDAO.FOR_VISIBLE_ID, id);
            } catch(Exception e) {
                log.error("Error looking up ID in database", e);
                continue;
            }
            
            if(p == null) {
                for(Player player : players) {
                    if(player.getVisibleID().equals(id)) {
                        p = player;
                        break;
                    }
                }
            }
                
            if(p == null) {
                p = new Player();
                p.setVisibleID(id);
            }

            PlayerDetails pd = null;
            pd = database.get(PlayerDetails.class, p.getDetailsID());
            if(pd == null) {
                pd = new PlayerDetails();
                p.setDetailsID(pd.getID());
            }

            /****************** update fields if supplied *********************/
            
            String firstName = row.get("firstName");
            if(firstName != null && firstName.length() > 0)
                p.setFirstName(firstName);
            
            String lastName = row.get("lastName");
            if(lastName != null && lastName.length() > 0)
                p.setLastName(lastName);

            String team = row.get("club");
            if(team != null && !team.isEmpty())
                p.setTeam(team);


            /**** Grade: try to handle lots of possibilities *****/
            try {
                String grade = row.get("grade");
                if(grade != null && !grade.isEmpty()) {

                    grade = grade.toUpperCase();
                    for(Grade g : Grade.values()) {
                        if(grade.equalsIgnoreCase(g.shortGrade) || grade.equalsIgnoreCase(g.veryShortGrade)) {
                            p.setGrade(g);
                            break;
                        } else if(grade.startsWith("BLACK")) {
                            if(g.toString().startsWith("BLACK") &&
                               grade.contains(g.toString().substring(6, 9))) {
                                    p.setGrade(g);
                                    break;
                            }
                        } else if(grade.startsWith(g.toString())) {
                            p.setGrade(g);
                            break;
                        }
                    }
                }
            } catch(Exception e) {}
            
            try {
                String gender = row.get("gender");
                if(gender.toUpperCase().startsWith("M")) {
                    p.setGender(Player.Gender.MALE);
                } else if(gender.toUpperCase().startsWith("F")) {
                    p.setGender(Player.Gender.FEMALE);
                }
            } catch(Exception e) {}
            
            try {
                p.setDob(new Date(dateFormat.parse(row.get("dob")).getTime()));
            } catch(Exception e) {}
            
            try {
                p.setWeight(Double.valueOf(row.get("weight")));
            } catch(Exception e) {}

            String division = row.get("division");
            if(division != null && division.length() > 0) {
                Pool pool = database.find(Pool.class, PoolDAO.BY_DESCRIPTION, division);
                if(pool != null) {
                    PlayerPool pp = new PlayerPool();
                    pp.setID(new PlayerPool.Key(p.getID(), pool.getID()));

                    try {
                        pp.setSeed(Integer.valueOf(row.get("seed")));
                    } catch(Exception e) {}

                    pps.add(pp);
                }
            }

            /****** Player details stuff ***********/

            String homeNumber = row.get("homeNumber");
            if(homeNumber != null && !homeNumber.isEmpty())
                pd.setHomeNumber(homeNumber);

            String workNumber = row.get("workNumber");
            if(workNumber != null && !workNumber.isEmpty())
                pd.setWorkNumber(workNumber);

            String mobileNumber = row.get("mobileNumber");
            if(mobileNumber != null && !mobileNumber.isEmpty())
                pd.setMobileNumber(mobileNumber);

            String street = row.get("street");
            if(street != null && !street.isEmpty())
                pd.setStreet(street);

            String city = row.get("city");
            if(city != null && !city.isEmpty())
                pd.setCity(city);

            String state = row.get("state");
            if(state != null && !state.isEmpty())
                pd.setState(state);

            String postcode = row.get("postcode");
            if(postcode != null && !postcode.isEmpty())
                pd.setPostcode(postcode);

            String email = row.get("email");
            if(email != null && !email.isEmpty())
                pd.setEmail(email);

            String emergencyName = row.get("emergencyName");
            if(emergencyName != null && !emergencyName.isEmpty())
                pd.setEmergencyName(emergencyName);

            String emergencyPhone = row.get("emergencyPhone");
            if(emergencyPhone != null && !emergencyPhone.isEmpty())
                pd.setEmergencyPhone(emergencyPhone);

            String emergencyMobile = row.get("emergencyMobile");
            if(emergencyMobile != null && !emergencyMobile.isEmpty())
                pd.setEmergencyMobile(emergencyMobile);

            String medicalInfo = row.get("medicalInfo");
            if(medicalInfo != null && !medicalInfo.isEmpty())
                pd.setMedicalInfo(medicalInfo);

            String medicalConditions = row.get("medicalConditions");
            if(medicalConditions != null && !medicalConditions.isEmpty())
                pd.setMedicalConditions(medicalConditions);

            String injuryInfo = row.get("injuryInfo");
            if(injuryInfo != null && !injuryInfo.isEmpty())
                pd.setInjuryInfo(injuryInfo);
            
            /************************* end field updates **********************/

            if(p.getVisibleID() == null || p.getVisibleID().isEmpty())
                throw new RuntimeException("Player ID missing in row " + rowNumber);
            if(p.getFirstName() == null || p.getFirstName().isEmpty() ||
               p.getLastName() == null || p.getLastName().isEmpty())
                throw new RuntimeException("Player first/last name missing in row " + rowNumber);
                        
            /* update database */
            if(!players.contains(p))
                players.add(p);

            pds.add(pd);
        }

        database.perform(new Transaction() {
            @Override
            public void perform() {
                for(Player player : players) {
                    try {
                        if(database.get(Player.class, player.getID()) == null)
                            database.add(player);
                        else
                            database.update(player);
                    } catch(Exception e) {
                        log.error("Exception while adding/updating player in database", e);
                    }
                }
                for(PlayerPool pp : pps) {
                    try {
                        PlayerPool currpp = database.get(PlayerPool.class, pp.getID());
                        if(currpp == null || !currpp.isValid()) {
                            database.update(pp);
                        }
                    } catch(Exception e) {
                        log.error("Exception while adding/updating player in database", e);
                    }
                }
                for(PlayerDetails pd : pds) {
                    try {
                        if(database.get(PlayerDetails.class, pd.getID()) == null)
                            database.add(pd);
                        else
                            database.update(pd);
                    } catch(Exception e) {
                        log.error("Exception while adding/updating player details in database", e);
                    }
                }
            }
        });
        
        return players.size();
    }

}
