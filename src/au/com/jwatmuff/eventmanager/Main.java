/*
 * Main.java
 *
 * Created on 30 April 2008, 17:12
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
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
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import org.apache.commons.io.FileUtils;
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
    public static final String VERSION = "8";
    public static final String WINDOW_TITLE = "Event Manager 2013 Update 1";
    public static final String VISIBLE_VERSION = "Event Manager 2013 Update 1";

    private static File workingDir = new File(".");

    private static final int LOCK_PORT = 58536;

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
    
    /**
     * Main method.
     */
    public static void main(String args[]) {
        LogUtils.setupUncaughtExceptionHandler();

        /* Set timeout for RMI connections - TODO: move to external file */
        System.setProperty("sun.rmi.transport.tcp.handshakeTimeout", "2000");
        try {
            //System.setProperty("java.rmi.server.hostname", InetAddress.getLocalHost().getHostName());
            System.setProperty("java.rmi.server.hostname", InetAddress.getLocalHost().getHostAddress());
        } catch(Exception e) {}
        
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
            if(!peerManager.initialisedOk()) {
                GUIUtils.displayMessage(loadWindow, "Event Manager could not find a network connection.\nTo connect to other computers you will need to establish a network connection and restart the Event Manager.", "No Network Connection");
            }

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

                    SynchronizingWindow syncWindow = new SynchronizingWindow();
                    syncWindow.setVisible(true);
                    DistributedDatabase database = databaseManager.activateDatabase(info.id, info.passwordHash);
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
