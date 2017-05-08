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

package au.com.jwatmuff.eventmanager.test;

import au.com.jwatmuff.eventmanager.db.PlayerDAO;
import au.com.jwatmuff.eventmanager.db.PlayerPoolDAO;
import au.com.jwatmuff.eventmanager.db.PoolDAO;
import au.com.jwatmuff.eventmanager.db.SessionDAO;
import au.com.jwatmuff.eventmanager.model.misc.AutoAssign;
import au.com.jwatmuff.eventmanager.model.misc.CSVImporter;
import au.com.jwatmuff.eventmanager.model.misc.DatabaseStateException;
import au.com.jwatmuff.eventmanager.model.misc.FightGenerator;
import au.com.jwatmuff.eventmanager.model.misc.PlayerLocker;
import au.com.jwatmuff.eventmanager.model.misc.PoolLocker;
import au.com.jwatmuff.eventmanager.model.misc.SessionLinker;
import au.com.jwatmuff.eventmanager.model.misc.SessionLocker;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.PlayerPool;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.model.vo.Session;
import au.com.jwatmuff.genericdb.p2p.DistributedDatabase;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.log4j.Logger;

/**
 * Helper class for semi-automated testing of EventManager.
 * 
 * Testing behaviour is enabled by supplying special options on the command line.
 *
 * -Dflaky=true  - Simulate random failures at critical points in the code.
 * 
 * -Drunscript=full - After selecting a database, runs a script that imports
 * players and divisions and simulates a full competition.
 * 
 * @author james
 */
public class TestUtil {
    public static final Logger log = Logger.getLogger(TestUtil.class);
    public static boolean SIMULATE_FLAKINESS = System.getProperty("flaky", "false").equals("true");
    public static String SCRIPT_TO_RUN = System.getProperty("runscript");

    public static void setActivatedDatabase(final TransactionalDatabase database) {
        if(SCRIPT_TO_RUN == null) return;
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                switch(SCRIPT_TO_RUN) {
                    case "full":
                        runScriptFull(database);
                        break;
                }
            }
        }).start();
    }
    
    static void sleep(long seconds) throws InterruptedException {
        log.debug("Waiting for " + seconds + " seconds");
        Thread.sleep(seconds * 1000);
    }
    
    static void runScriptFull(TransactionalDatabase database) {
        try {
            log.debug("Running script 'full'");

            sleep(5);
            
            log.debug("Importing players");
            CSVImporter.importPlayers(new File("../../../test/player_test_data.csv"), database);
            
            sleep(2);
            
            log.debug("Importing divisions");
            CSVImporter.importPools(new File("../../../test/pool_test_data.csv"), database);
            
            sleep(2);
            
            log.debug("Lock players");
            // lock all players so they can be assigned to pools
            for(Player p : database.findAll(Player.class, PlayerDAO.ALL)) {
                p.setWeight(Math.random() * 20 + 50);
                database.update(p);
                PlayerLocker.lockPlayer(database, p);
            }
            
            sleep(2);

            log.debug("Auto assign players");
            AutoAssign.assignPlayersToPools(database);
        
            sleep(2);
            
            
            for(Pool pool : database.findAll(Pool.class, PoolDAO.WITH_PLAYERS)) {
                log.debug("Approve & Lock pools");
                for(PlayerPool pp : database.findAll(PlayerPool.class, PlayerPoolDAO.FOR_POOL, pool.getID())) {
                    pp.setApproved(true);
                    database.update(pp);
                }
                pool = PoolLocker.lockPoolPlayers(database, pool);

                sleep(2);

                log.debug("Generate fights");
                FightGenerator.generateFightsForPool(database, pool);

                sleep(2);

                log.debug("Lock fights");
                pool = PoolLocker.lockPoolFights(database, pool);

                sleep(2);
            }
            
            log.debug("Create sessions");
            Session mat = new Session();
            mat.setType(Session.SessionType.MAT);
            mat.setMat("Mat");
            database.add(mat);

            Session session = new Session();
            session.setType(Session.SessionType.NORMAL);
            session.setName("Session");

            Collection<Session> preceding = new ArrayList<Session>();
            Collection<Pool> pools = database.findAll(Pool.class, PoolDAO.WITHOUT_SESSION);

            SessionLinker.insertSession(database, session, mat, preceding, pools);
            
            sleep(2);
            
            log.debug("Lock session position");
            
            SessionLocker.lockPosition(database, session);
            
            sleep(2);
            
            log.debug("Lock session fights");
            
            for(Session session2 : database.findAll(Session.class, SessionDAO.WITH_LOCKED_STATUS, Session.LockedStatus.POSITION_LOCKED)) {
                SessionLocker.lockFights(database, session2);
            }
            
            sleep(2);
        } catch(InterruptedException | IOException | DatabaseStateException e) {
            log.debug("Error during script", e);
        }
    }
}
