/*
 * SessionLockerTest.java
 * JUnit based test
 *
 * Created on 29 August 2008, 16:39
 */

package au.com.jwatmuff.eventmanager.model.misc;

import au.com.jwatmuff.eventmanager.db.PlayerDAO;
import au.com.jwatmuff.eventmanager.db.PlayerPoolDAO;
import au.com.jwatmuff.eventmanager.db.PoolDAO;
import au.com.jwatmuff.eventmanager.db.SessionDAO;
import au.com.jwatmuff.eventmanager.db.sqlite.SQLiteDatabase;
import au.com.jwatmuff.eventmanager.model.vo.CompetitionInfo;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.PlayerPool;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import junit.framework.*;
import au.com.jwatmuff.eventmanager.model.vo.Session;
import au.com.jwatmuff.genericdb.transaction.NotifyingTransactionalDatabase;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabaseUpdater;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import javax.sql.DataSource;
import org.apache.commons.dbcp.BasicDataSource;

/**
 *
 * @author James
 */
public class SessionLockerTest extends TestCase {
    private DataSource dataSource;

    public SessionLockerTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        System.out.println("setup");
        
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName("org.sqlite.JDBC");
        ds.setUrl("jdbc:sqlite::memory:");
        this.dataSource = ds;
    }

    @Override
    protected void tearDown() throws Exception {
        
    }
    
    private TransactionalDatabase getNewDatabase() {
        SQLiteDatabase db = new SQLiteDatabase();
        db.setDataSource(dataSource);
        db.afterPropertiesSet();
        
        NotifyingTransactionalDatabase ndbA = new NotifyingTransactionalDatabase(db);
        ndbA.setReadOnly(true);
        ndbA.setUpdateTimestamps(true);
        NotifyingTransactionalDatabase ndbB = new NotifyingTransactionalDatabase(db);
        ndbB.setReadOnly(false);
        ndbB.setUpdateTimestamps(false);
        TransactionalDatabaseUpdater updater = new TransactionalDatabaseUpdater(ndbB);

        ndbA.setListener(updater);
        
        return ndbA;
    }
    
    private static Session setupUnlockedSession(TransactionalDatabase database) throws IOException {
        Session session = null;
        try {
            CSVImporter.importPlayers(new File("test/player_test_data.csv"), database);
            CSVImporter.importPools(new File("test/pool_test_data.csv"), database);

            CompetitionInfo ci = new CompetitionInfo();
            Calendar cal = new GregorianCalendar(2008, 1, 1);
            ci.setStartDate(cal.getTime());
            cal.roll(GregorianCalendar.DATE, 2);
            ci.setEndDate(cal.getTime());
            ci.setAgeThresholdDate(cal.getTime());
            ci.setName("My Comp");
            ci.setMats(1);
            database.add(ci);

            // lock all players so they can be assigned to pools
            for(Player p : database.findAll(Player.class, PlayerDAO.ALL)) {
                p.setWeight(65.0);
                database.update(p);
                PlayerLocker.lockPlayer(database, p);
            }

            AutoAssign.assignPlayersToPools(database);
        
            Pool pool = database.findAll(Pool.class, PoolDAO.WITH_PLAYERS).iterator().next();
            for(PlayerPool pp : database.findAll(PlayerPool.class, PlayerPoolDAO.FOR_POOL, pool.getID())) {
                pp.setApproved(true);
                database.update(pp);
            }
            pool = PoolLocker.lockPoolPlayers(database, pool);
            FightGenerator.generateFightsForPool(database, pool);

            pool = PoolLocker.lockPoolFights(database, pool);


            Session mat = new Session();
            mat.setType(Session.SessionType.MAT);
            mat.setMat("Mat");
            database.add(mat);

            session = new Session();
            session.setType(Session.SessionType.NORMAL);
            session.setName("Session");

            Collection<Session> preceding = new ArrayList<Session>();
            Collection<Pool> pools = new ArrayList<Pool>();
            pools.add(pool);

            SessionLinker.insertSession(database, session, mat, preceding, pools);
        } catch (DatabaseStateException ex) {
            fail(ex.getMessage());
        }
        
        return session;
    }

    /**
     * Test of lockPosition method, of class au.com.jwatmuff.eventmanager.model.misc.SessionLocker.
     */
    public void testLockPosition() {
        System.out.println("lockPosition");
        
        TransactionalDatabase database = getNewDatabase();
        Session session;
        try {
            session = setupUnlockedSession(database);
        } catch(IOException e) {
            fail("Exception importing test data: " + e.getMessage());
            return;
        }
        
        try {
            SessionLocker.lockPosition(database, session);
        } catch (DatabaseStateException e) {
            fail("Exception locking position: " + e.getMessage());
        }
        
        Collection<Session> locked = database.findAll(Session.class, SessionDAO.WITH_LOCKED_STATUS, Session.LockedStatus.POSITION_LOCKED);
        
        assertEquals(locked.size(), 2);
        assertEquals(locked.iterator().next().getLockedStatus(), Session.LockedStatus.POSITION_LOCKED);
    }

    /**
     * Test of lockFights method, of class au.com.jwatmuff.eventmanager.model.misc.SessionLocker.
     */
    public void testLockFights() {
        System.out.println("lockFights");
        
        TransactionalDatabase database = getNewDatabase();
        Session session;
        try {
            session = setupUnlockedSession(database);
        } catch(IOException e) {
            fail("Exception importing test data: " + e.getMessage());
            return;
        }

        try {
            SessionLocker.lockPosition(database, session);
        } catch (DatabaseStateException e) {
            fail("Exception locking position: " + e.getMessage());
        }

        Collection<Session> locked = database.findAll(Session.class, SessionDAO.WITH_LOCKED_STATUS, Session.LockedStatus.POSITION_LOCKED);
        session = locked.iterator().next();

        try {
            SessionLocker.lockFights(database, session);
        } catch (DatabaseStateException e) {
            fail("Exception locking fights: " + e.getMessage());
        }
        
        locked = database.findAll(Session.class, SessionDAO.WITH_LOCKED_STATUS, Session.LockedStatus.FIGHTS_LOCKED);
        assertTrue(locked.size() >= 1);
        assertEquals(locked.iterator().next().getLockedStatus(), Session.LockedStatus.FIGHTS_LOCKED);
    }   
}
