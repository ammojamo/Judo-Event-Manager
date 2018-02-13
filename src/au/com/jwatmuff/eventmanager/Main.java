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

package au.com.jwatmuff.eventmanager;

import au.com.jwatmuff.eventmanager.db.sqlite.SQLiteDatabaseManager;
import au.com.jwatmuff.eventmanager.gui.main.LoadCompetitionWindow;
import au.com.jwatmuff.eventmanager.gui.main.LoadWindow;
import au.com.jwatmuff.eventmanager.gui.main.MainWindow;
import au.com.jwatmuff.eventmanager.gui.main.SynchronizingWindow;
import au.com.jwatmuff.eventmanager.model.vo.CompetitionInfo;
import au.com.jwatmuff.eventmanager.permissions.License;
import au.com.jwatmuff.eventmanager.permissions.LicenseManager;
import au.com.jwatmuff.eventmanager.permissions.LicenseType;
import au.com.jwatmuff.eventmanager.permissions.PermissionChecker;
import au.com.jwatmuff.eventmanager.test.TestUtil;
import au.com.jwatmuff.eventmanager.util.GUIUtils;
import au.com.jwatmuff.eventmanager.util.LogUtils;
import au.com.jwatmuff.genericdb.p2p.DatabaseInfo;
import au.com.jwatmuff.genericdb.p2p.DatabaseManager;
import au.com.jwatmuff.genericdb.p2p.DistributedDatabase;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import au.com.jwatmuff.genericp2p.JmDNSRMIPeerManager;
import au.com.jwatmuff.genericp2p.ManualDiscoveryService;
import au.com.jwatmuff.genericp2p.PeerManager;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

/**
 *
 * @author James
 */
public class Main {
    private static Logger log = Logger.getLogger(Main.class);

    //internal version - bump this up whenever making a database change
    public static final String VERSION = "10";
    public static final String WINDOW_TITLE = "Event Manager 2018 Update 1";
    public static final String VISIBLE_VERSION = "Event Manager 2018 Update 1";

    private static File workingDir = new File(".");

    private static final int LOCK_PORT = Integer.valueOf(System.getProperty("eventmanager.lockport", "58536")); // 58536;

    /**
     * Binds to a TCP port to ensure we are the only instance of Event Manager
     * running on the computer. Returns false if the TCP port is already bound
     * and the force parameter is set to false.
     */
    private static boolean obtainRunLock(boolean force) {
        if(force) return true;
        try {
            final ServerSocket lockSocket = new ServerSocket(LOCK_PORT);
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        lockSocket.close();
                    } catch(IOException e) {
                        log.warn("Failed to close lock socket", e);
                    }
                }
            });
        } catch(IOException e) {
            log.info("Run lock is present");
            return false;
        }

        return true;
    }

    public static File getWorkingDirectory() {
        return workingDir;
    }

    private static boolean updateRmiHostName() {
        try {
            String existingHostName = System.getProperty("java.rmi.server.hostname");
            String newHostName = InetAddress.getLocalHost().getHostAddress();
            if(!ObjectUtils.equals(existingHostName, newHostName)) {
                log.info("Updating RMI hostname to: " + newHostName);
                System.setProperty("java.rmi.server.hostname", newHostName);
                return true;
            }
        } catch(Exception e) {}
        return false;
    }

    private static void monitorNetworkInterfaceChanges(final PeerManager peerManager) {
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                if(updateRmiHostName()) {
                    peerManager.refreshServices();
                }
            }
        }, 15, 15, TimeUnit.SECONDS);
    }

    /**
     * Main method.
     */
    public static void main(String args[]) {
        LogUtils.setupUncaughtExceptionHandler();

        /* Set timeout for RMI connections - TODO: move to external file */
        System.setProperty("sun.rmi.transport.tcp.handshakeTimeout", "2000");
        updateRmiHostName();

        /*
         * Set up menu bar for Mac
         */
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Event Manager");

        /*
         * Set look and feel to 'system' style
         */
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e) {
           log.info("Failed to set system look and feel");
        }

        /*
         * Set workingDir to a writable folder for storing competitions, settings etc.
         */
        String applicationData = System.getenv("APPDATA");
        if(applicationData != null) {
            workingDir = new File(applicationData, "EventManager");
            if(workingDir.exists() || workingDir.mkdirs()) {
                // redirect logging to writable folder
                LogUtils.reconfigureFileAppenders("log4j.properties", workingDir);
            } else {
                workingDir = new File(".");
            }
        }

        // log version for debugging
        log.info("Running version: " + VISIBLE_VERSION + " (Internal: " + VERSION + ")");

        /*
         * Copy license if necessary
         */
        File license1 = new File("license.lic");
        File license2 = new File(workingDir, "license.lic");
        if(license1.exists() && !license2.exists()) {
            try {
                FileUtils.copyFile(license1, license2);
            } catch(IOException e) {
                log.warn("Failed to copy license from " + license1 + " to " + license2, e);
            }
        }
        if(license1.exists() && license2.exists()) {
            if(license1.lastModified() > license2.lastModified()) {
                try {
                    FileUtils.copyFile(license1, license2);
                } catch(IOException e) {
                    log.warn("Failed to copy license from " + license1 + " to " + license2, e);
                }
            }
        }

        /*
         * Check if run lock exists, if so ask user if it is ok to continue
         */
        if(!obtainRunLock(false)) {
            int response = JOptionPane.showConfirmDialog(
                    null,
                    "Unable to obtain run-lock.\nPlease ensure that no other instances of EventManager are running before continuing.\nDo you wish to continue?",
                    "Run-lock detected",
                    JOptionPane.YES_NO_OPTION);
            if(response == JOptionPane.YES_OPTION)
                obtainRunLock(true);
            else
                System.exit(0);
        }

        try {
            LoadWindow loadWindow = new LoadWindow();
            loadWindow.setVisible(true);

            loadWindow.addMessage("Reading settings..");

            /*
             * Read properties from file
             */
            final Properties props = new Properties();
            try {
                Properties defaultProps = PropertiesLoaderUtils.loadProperties(new ClassPathResource("eventmanager.properties"));
                props.putAll(defaultProps);
            } catch (IOException ex) {
                log.error(ex);
            }

            props.putAll(System.getProperties());

            File databaseStore = new File(workingDir, "comps");
            int rmiPort = Integer.parseInt(props.getProperty("eventmanager.rmi.port"));

            loadWindow.addMessage("Loading Peer Manager..");
            log.info("Loading Peer Manager");

            ManualDiscoveryService manualDiscoveryService = new ManualDiscoveryService();
            JmDNSRMIPeerManager peerManager = new JmDNSRMIPeerManager(rmiPort, new File(workingDir, "peerid.dat"));
            peerManager.addDiscoveryService(manualDiscoveryService);

            monitorNetworkInterfaceChanges(peerManager);

            loadWindow.addMessage("Loading Database Manager..");
            log.info("Loading Database Manager");

            DatabaseManager databaseManager = new SQLiteDatabaseManager(databaseStore, peerManager);
            LicenseManager licenseManager = new LicenseManager(workingDir);

            loadWindow.addMessage("Loading Load Competition Dialog..");
            log.info("Loading Load Competition Dialog");

            LoadCompetitionWindow loadCompetitionWindow
                    = new LoadCompetitionWindow(databaseManager, licenseManager, peerManager);
            loadCompetitionWindow.setTitle(WINDOW_TITLE);

            loadWindow.dispose();
            log.info("Starting Load Competition Dialog");

            while(true) {
                // reset permission checker to use our license
                licenseManager.updatePermissionChecker();

                GUIUtils.runModalJFrame(loadCompetitionWindow);

                if(loadCompetitionWindow.getSuccess()) {
                    DatabaseInfo info = loadCompetitionWindow.getSelectedDatabaseInfo();

                    if(!databaseManager.checkLock(info.id)) {
                        String message = "EventManager did not shut down correctly the last time this competition was open. To avoid potential data corruption, you are advised to take the following steps:\n" +
                                "1) Do NOT open this competition\n" +
                                "2) Create a backup of this competition\n" +
                                "3) Delete the competition from this computer\n" +
                                "4) If possible, reload this competition from another computer on the network\n" +
                                "5) Alternatively, you may manually load the backup onto all computers on the network, ensuring the 'Preserve competition ID' option is checked.";
                        String title = "WARNING: Potential Data Corruption Detected";

                        int status = JOptionPane.showOptionDialog(
                                null,
                                message,
                                title,
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.ERROR_MESSAGE,
                                null,
                                new Object[] { "Cancel (recommended)", "Open anyway" },
                                "Cancel (recommended)");

                        if(status == 0) continue; // return to load competition window
                    }

                    SynchronizingWindow syncWindow = new SynchronizingWindow();
                    syncWindow.setVisible(true);
                    long t = System.nanoTime();
                    DistributedDatabase database = databaseManager.activateDatabase(info.id, info.passwordHash);
                    long dt = System.nanoTime() - t;
                    log.debug(String.format("Initial sync in %dms", TimeUnit.MILLISECONDS.convert(dt, TimeUnit.NANOSECONDS)));
                    syncWindow.dispose();

                    if(loadCompetitionWindow.isNewDatabase()) {
                        GregorianCalendar calendar = new GregorianCalendar();
                        Date today = calendar.getTime();
                        calendar.set(Calendar.MONTH, Calendar.DECEMBER);
                        calendar.set(Calendar.DAY_OF_MONTH, 31);
                        Date endOfYear = new java.sql.Date(calendar.getTimeInMillis());

                        CompetitionInfo ci = new CompetitionInfo();
                        ci.setName(info.name);
                        ci.setStartDate(today);
                        ci.setEndDate(today);
                        ci.setAgeThresholdDate(endOfYear);
                        //ci.setPasswordHash(info.passwordHash);
                        License license = licenseManager.getLicense();
                        if(license != null) {
                            ci.setLicenseName(license.getName());
                            ci.setLicenseType(license.getType().toString());
                            ci.setLicenseContact(license.getContactPhoneNumber());
                        }
                        database.add(ci);
                    }

                    // Set PermissionChecker to use database's license type
                    String competitionLicenseType = database.get(CompetitionInfo.class, null).getLicenseType();
                    PermissionChecker.setLicenseType(LicenseType.valueOf(competitionLicenseType));

                    TransactionNotifier notifier = new TransactionNotifier();
                    database.setListener(notifier);

                    MainWindow mainWindow = new MainWindow();
                    mainWindow.setDatabase(database);
                    mainWindow.setNotifier(notifier);
                    mainWindow.setPeerManager(peerManager);
                    mainWindow.setLicenseManager(licenseManager);
                    mainWindow.setManualDiscoveryService(manualDiscoveryService);
                    mainWindow.setTitle(WINDOW_TITLE);
                    mainWindow.afterPropertiesSet();

                    TestUtil.setActivatedDatabase(database);

                    // show main window (modally)
                    GUIUtils.runModalJFrame(mainWindow);

                    // shutdown procedures

                    // System.exit();

                    database.shutdown();
                    databaseManager.deactivateDatabase(1500);

                    if(mainWindow.getDeleteOnExit()) {
                        for(File file : info.localDirectory.listFiles())
                            if(!file.isDirectory())
                                file.delete();
                        info.localDirectory.deleteOnExit();
                    }
                } else {
                    // This can cause an RuntimeException - Peer is disconnected
                    peerManager.stop();
                    System.exit(0);
                }
            }
        } catch(Throwable e) {
            log.error("Error in main function", e);
            String message = e.getMessage();
            if(message == null) message = "";
            if(message.length() > 100)
                message = message.substring(0,97) + "...";
            GUIUtils.displayError(null, "An unexpected error has occured.\n\n" + e.getClass().getSimpleName() + "\n" + message);
            System.exit(0);
        }
    }
}
